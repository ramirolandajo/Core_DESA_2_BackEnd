package ar.edu.uade.core.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ar.edu.uade.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.uade.core.repository.DeadLetterRepository;
import ar.edu.uade.core.repository.EventRepository;
import ar.edu.uade.core.repository.LiveMessageRepository;
import ar.edu.uade.core.repository.MessageConsumptionRepository;
import ar.edu.uade.core.repository.RetryMessageRepository;

import jakarta.transaction.Transactional;

@Service
public class KafkaMockService {

    private static final Logger log = LoggerFactory.getLogger(KafkaMockService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LiveMessageRepository liveMessageRepository;

    @Autowired
    private RetryMessageRepository retryMessageRepository;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Autowired
    private MessageConsumptionRepository consumptionRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TopicResolver topicResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int defaultMaxAttempts = 3;
    private final long defaultRetryDelaySeconds = 30;
    private final long defaultTtlSeconds = 60 * 60;
    private final int distinctConsumptionThreshold = 2;

    // Config reintentos de envío a Kafka (fail-fast)
    @Value("${app.kafka.send.max-attempts:3}")
    private int sendMaxAttempts;
    @Value("${app.kafka.send.backoff-ms:500}")
    private long sendBackoffMs;
    @Value("${app.kafka.send.timeout-ms:5000}")
    private long sendTimeoutMs;

    // ----------------- event & live creation (from middleware) -----------------
    private Event persistEvent(String type, String payload, String originModule, LocalDateTime ts) {
        Event event = new Event();
        event.setType(type);
        event.setPayload(payload);
        event.setOriginModule(originModule);
        event.setTimestamp(ts != null ? ts : LocalDateTime.now());
        return eventRepository.save(event);
    }

    public Event ingestEvent(EventRequest req){
        if (req == null) throw new IllegalArgumentException("Request is null");
        if (req.getType() == null || req.getType().isBlank()) throw new IllegalArgumentException("type is required");
        if (req.getOriginModule() == null || req.getOriginModule().isBlank()) throw new IllegalArgumentException("originModule is required");
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(req.getPayload());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando payload", e);
        }
        Event event = persistEvent(req.getType(), payloadJson, req.getOriginModule(), req.getTimestamp());

        // crear live message para seguimiento interno
        LiveMessage lm = new LiveMessage();
        lm.setEventId(event.getId());
        lm.setType(event.getType());
        lm.setPayload(event.getPayload());
        lm.setTimestamp(event.getTimestamp());
        lm.setOriginModule(event.getOriginModule());
        liveMessageRepository.save(lm);

        // publicar en Kafka con envelope estandarizado
        String topic = topicResolver.resolveTopic(req.getType(), req.getOriginModule());
        EventMessage msg = new EventMessage(
                UUID.randomUUID().toString(),
                req.getType(), // eventType mantiene el tipo original
                OffsetDateTime.now(ZoneOffset.UTC),
                req.getOriginModule(),
                req.getPayload()
        );
        // Envío síncrono con reintentos limitados
        sendToKafkaWithRetries(topic, msg.getEventId(), msg);
        log.info("Event {} publicado en topic {} (msgId={})", event.getId(), topic, msg.getEventId());
        return event;
    }

    private void sendToKafkaWithRetries(String topic, String key, Object value){
        int attempt = 0;
        Exception last = null;
        while (attempt < sendMaxAttempts){
            attempt++;
            try {
                log.debug("[KafkaSend] Enviando a topic='{}' intento {}/{}", topic, attempt, sendMaxAttempts);
                CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, value);
                // esperar resultado con timeout para fail-fast
                SendResult<String, Object> res = future.get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                if (res != null) {
                    return; // OK
                }
            } catch (Exception ex){
                last = ex;
                log.warn("[KafkaSend] Fallo en envio a '{}' intento {}/{}: {}", topic, attempt, sendMaxAttempts, ex.getMessage());
                if (attempt < sendMaxAttempts){
                    try { Thread.sleep(sendBackoffMs); } catch (InterruptedException ie){ Thread.currentThread().interrupt(); }
                }
            }
        }
        String err = String.format("No se pudo publicar en Kafka tras %d intentos (topic=%s)", sendMaxAttempts, topic);
        log.error("[KafkaSend] {}", err, last);
        throw new IllegalStateException(err, last);
    }

    public List<Event> getAll(){
        return eventRepository.findAll();
    }

    // ----------------- listas -----------------
    public List<LiveMessage> getLiveMessages(){ return liveMessageRepository.findAll(); }
    public List<RetryMessage> getRetryMessages(){ return retryMessageRepository.findAll(); }
    public List<DeadLetterMessage> getDeadLetters(){ return deadLetterRepository.findAll(); }

    public void consumeAllLive(){ liveMessageRepository.deleteAll(); }

    // Lookup diagnóstico: devuelve dónde está el mensaje (LIVE / RETRY / DEAD / NOT_FOUND)
    public ar.edu.uade.core.model.MessageLookupResult messageLookup(Integer liveId, Integer eventId){
        ar.edu.uade.core.model.MessageLookupResult res = new ar.edu.uade.core.model.MessageLookupResult();
        // buscar en live
        LiveMessage lm = null;
        if (liveId != null) lm = liveMessageRepository.findById(liveId).orElse(null);
        if (lm == null && eventId != null) lm = liveMessageRepository.findByEventId(eventId).orElse(null);
        if (lm != null){
            res.setLocation("LIVE");
            res.setLiveId(lm.getId());
            res.setEventId(lm.getEventId());
            res.setOriginModule(lm.getOriginModule());
            res.setType(lm.getType());
            res.setPayload(lm.getPayload());
            return res;
        }

        // buscar en retries
        RetryMessage rm = null;
        if (liveId != null) rm = retryMessageRepository.findByOriginalLiveId(liveId).orElse(null);
        if (rm == null && liveId != null) rm = retryMessageRepository.findById(liveId).orElse(null); // caso cliente pasó retryId
        if (rm == null && eventId != null) rm = retryMessageRepository.findByEventId(eventId).orElse(null);
        if (rm == null && eventId == null && liveId != null) rm = retryMessageRepository.findByEventId(liveId).orElse(null);
        if (rm == null){
            // fallback: buscar consumptions previas por liveMessageId y usar eventId encontrado
            if (liveId != null){
                var consumptions = consumptionRepository.findByLiveMessageId(liveId);
                if (consumptions != null && !consumptions.isEmpty()){
                    Integer evt = consumptions.get(0).getEventId();
                    rm = retryMessageRepository.findByEventId(evt).orElse(null);
                }
            }
        }
        if (rm != null){
            res.setLocation("RETRY");
            res.setRetryId(rm.getId());
            res.setEventId(rm.getEventId());
            res.setType(rm.getType());
            res.setPayload(rm.getPayload());
            return res;
        }

        // buscar en dead letters por eventId
        if (eventId != null){
            for(DeadLetterMessage dm: deadLetterRepository.findAll()){
                if (dm.getEventId() != null && dm.getEventId().equals(eventId)){
                    res.setLocation("DEAD");
                    res.setEventId(dm.getEventId());
                    res.setType(dm.getType());
                    res.setPayload(dm.getPayload());
                    return res;
                }
            }
        }

        res.setLocation("NOT_FOUND");
        return res;
    }

    // Obtener consumptions para auditar
    public List<MessageConsumption> getConsumptions(Integer liveId, Integer eventId){
        if (liveId != null) return consumptionRepository.findByLiveMessageId(liveId);
        if (eventId != null) return consumptionRepository.findByEventId(eventId);
        return consumptionRepository.findAll();
    }

    // ----------------- consumo (core) -----------------
    @Transactional
    public ConsumeResult consumeOneAndMoveToRetry(Integer liveMessageId, Integer eventId, String consumerModule){
        log.debug("consumeOne called with liveMessageId={} eventId={} module={}", liveMessageId, eventId, consumerModule);

        // 1) intentar localizar live message
        LiveMessage lm = null;
        if (liveMessageId != null) lm = liveMessageRepository.findById(liveMessageId).orElse(null);
        if (lm == null && eventId != null) lm = liveMessageRepository.findByEventId(eventId).orElse(null);

        // 2) si no hay live, intentar localizar retry por varios caminos
        if (lm == null){
            RetryMessage rm = null;
            if (liveMessageId != null) rm = retryMessageRepository.findByOriginalLiveId(liveMessageId).orElse(null);
            if (rm == null && liveMessageId != null) rm = retryMessageRepository.findById(liveMessageId).orElse(null); // caso cliente pasó retryId
            if (rm == null && eventId != null) rm = retryMessageRepository.findByEventId(eventId).orElse(null);
            if (rm == null && eventId == null && liveMessageId != null) rm = retryMessageRepository.findByEventId(liveMessageId).orElse(null);
            if (rm == null){
                // fallback: buscar consumptions previas por liveMessageId y usar eventId encontrado
                if (liveMessageId != null){
                    var consumptions = consumptionRepository.findByLiveMessageId(liveMessageId);
                    if (consumptions != null && !consumptions.isEmpty()){
                        Integer evt = consumptions.get(0).getEventId();
                        rm = retryMessageRepository.findByEventId(evt).orElse(null);
                    }
                }
            }
            if (rm == null) return ConsumeResult.ofNotFound();

            // validar origen y duplicados
            Event ev = eventRepository.findById(rm.getEventId()).orElse(null);
            String origin = ev != null ? ev.getOriginModule() : null;
            if (origin != null && consumerModule != null && consumerModule.equalsIgnoreCase(origin)) return ConsumeResult.ofConflict("Consumer module cannot be the same as origin module");
            boolean alreadyConsumed = consumptionRepository.existsByEventIdAndModuleName(rm.getEventId(), consumerModule);
            if (alreadyConsumed) return ConsumeResult.ofConflict("Already consumed by module");

            // registrar consumo
            MessageConsumption mc = new MessageConsumption();
            mc.setEventId(rm.getEventId());
            mc.setLiveMessageId(rm.getId());
            mc.setModuleName(consumerModule);
            mc.setConsumedAt(LocalDateTime.now());
            consumptionRepository.save(mc);

            // verificar si alcanza threshold para dead-letter
            Set<String> distinctModules = consumptionRepository.findByEventId(rm.getEventId()).stream()
                    .map(MessageConsumption::getModuleName).collect(Collectors.toSet());
            if (distinctModules.size() >= distinctConsumptionThreshold){
                DeadLetterMessage dm = new DeadLetterMessage();
                dm.setEventId(rm.getEventId());
                dm.setType(rm.getType());
                dm.setPayload(rm.getPayload());
                dm.setReason("CONSUMED_BY_" + distinctModules.size() + "_MODULES");
                dm.setMovedAt(LocalDateTime.now());
                deadLetterRepository.save(dm);
                retryMessageRepository.deleteById(rm.getId());
                return ConsumeResult.ofDead(dm);
            }

            // incrementar intentos y actualizar nextAttemptAt
            int attempts = rm.getAttempts() == null ? 0 : rm.getAttempts();
            rm.setAttempts(attempts + 1);
            rm.setNextAttemptAt(LocalDateTime.now().plusSeconds(defaultRetryDelaySeconds));
            retryMessageRepository.save(rm);
            return ConsumeResult.ofRetry(rm);
        }

        // 3) si hay live message: validar origen y duplicados
        if (consumerModule != null && consumerModule.equalsIgnoreCase(lm.getOriginModule())) return ConsumeResult.ofConflict("Consumer module cannot be the same as origin module");
        boolean alreadyConsumedLive = consumptionRepository.existsByEventIdAndModuleName(lm.getEventId(), consumerModule);
        if (alreadyConsumedLive) return ConsumeResult.ofConflict("Already consumed by module");

        // registrar consumo desde live
        MessageConsumption mcLive = new MessageConsumption();
        mcLive.setEventId(lm.getEventId());
        mcLive.setLiveMessageId(lm.getId());
        mcLive.setModuleName(consumerModule);
        mcLive.setConsumedAt(LocalDateTime.now());
        consumptionRepository.save(mcLive);

        // verificar threshold
        Set<String> distinctLive = consumptionRepository.findByEventId(lm.getEventId()).stream()
                .map(MessageConsumption::getModuleName).collect(Collectors.toSet());
        if (distinctLive.size() >= distinctConsumptionThreshold){
            DeadLetterMessage dm = new DeadLetterMessage();
            dm.setEventId(lm.getEventId());
            dm.setType(lm.getType());
            dm.setPayload(lm.getPayload());
            dm.setReason("CONSUMED_BY_" + distinctLive.size() + "_MODULES");
            dm.setMovedAt(LocalDateTime.now());
            deadLetterRepository.save(dm);
            liveMessageRepository.deleteById(lm.getId());
            return ConsumeResult.ofDead(dm);
        }

        // crear retry a partir de live
        RetryMessage newRm = new RetryMessage();
        newRm.setEventId(lm.getEventId());
        newRm.setOriginalLiveId(lm.getId());
        newRm.setType(lm.getType());
        newRm.setPayload(lm.getPayload());
        newRm.setAttempts(1);
        newRm.setCreatedAt(LocalDateTime.now());
        newRm.setNextAttemptAt(LocalDateTime.now().plusSeconds(defaultRetryDelaySeconds));
        newRm.setMaxAttempts(defaultMaxAttempts);
        newRm.setTtlSeconds(defaultTtlSeconds);
        newRm.setConsumerModule(consumerModule);
        retryMessageRepository.save(newRm);
        liveMessageRepository.deleteById(lm.getId());
        return ConsumeResult.ofRetry(newRm);
    }

    @Transactional
    public boolean acknowledgeRetry(Integer retryId){
        if (!retryMessageRepository.existsById(retryId)) return false;
        retryMessageRepository.deleteById(retryId);
        return true;
    }

    @Transactional
    public ConsumeResult failRetry(Integer retryId){
        RetryMessage rm = retryMessageRepository.findById(retryId).orElse(null);
        if (rm == null) return ConsumeResult.ofNotFound();
        int attempts = rm.getAttempts() == null ? 0 : rm.getAttempts();
        attempts++;
        rm.setAttempts(attempts);
        if (attempts >= rm.getMaxAttempts()){
            DeadLetterMessage dm = new DeadLetterMessage();
            dm.setEventId(rm.getEventId());
            dm.setType(rm.getType());
            dm.setPayload(rm.getPayload());
            dm.setReason("MAX_ATTEMPTS_EXCEEDED");
            dm.setMovedAt(LocalDateTime.now());
            deadLetterRepository.save(dm);
            retryMessageRepository.deleteById(retryId);
            return ConsumeResult.ofDead(dm);
        } else {
            rm.setNextAttemptAt(LocalDateTime.now().plusSeconds(defaultRetryDelaySeconds));
            retryMessageRepository.save(rm);
            return ConsumeResult.ofRetry(rm);
        }
    }

    @Scheduled(fixedDelayString = "30000")
    public void scheduledProcessRetries(){ processRetriesAndExpire(); }

    public void processRetriesAndExpire(){
        List<RetryMessage> retries = retryMessageRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (RetryMessage r: retries){
            boolean expiredByTtl = r.getCreatedAt() != null && r.getCreatedAt().plusSeconds(r.getTtlSeconds()).isBefore(now);
            if (r.getAttempts() >= r.getMaxAttempts() || expiredByTtl){
                DeadLetterMessage dm = new DeadLetterMessage();
                dm.setEventId(r.getEventId());
                dm.setType(r.getType());
                dm.setPayload(r.getPayload());
                String reason = r.getAttempts() >= r.getMaxAttempts() ? "MAX_ATTEMPTS_EXCEEDED" : "TTL_EXPIRED";
                dm.setReason(reason);
                dm.setMovedAt(now);
                deadLetterRepository.save(dm);
                retryMessageRepository.deleteById(r.getId());
            }
        }
    }
    public void handleAcknowledgement(AcknowledgementRequest ack) {
        Integer eventId = ack.getEventId();
        Integer liveMessageId = ack.getLiveMessageId();
        String moduleName = ack.getModuleName();
        String status = ack.getStatus();

        log.info("[Core] ACK recibido --> eventId={} | liveMessageId={} | módulo={} | status={}",
                eventId, liveMessageId, moduleName, status);

        // Registrar consumo
        MessageConsumption mc = new MessageConsumption();
        mc.setEventId(eventId);
        mc.setLiveMessageId(liveMessageId);
        mc.setModuleName(moduleName);
        mc.setConsumedAt(LocalDateTime.now());
        consumptionRepository.save(mc);

        Optional<LiveMessage> liveOpt = liveMessageRepository.findById(liveMessageId);
        if (liveOpt.isEmpty()) {
            log.warn("[Core] No se encontró LiveMessage {} para ACK.", liveMessageId);
            return;
        }

        LiveMessage live = liveOpt.get();
        String eventType = live.getType();

        // Si status = "FAILED" lo paso a la cola de reintento
        if ("FAIL".equalsIgnoreCase(status)) {
            moveMessageToRetry(liveMessageId, moduleName);
            return;
        }

        // Si todos los modulos que tenian que consumir consumieron lo paso a cola de muertos y borro de live
        if (allModulesConsumedSuccessfully(eventId, eventType)) {
            DeadLetterMessage dm = new DeadLetterMessage();
            dm.setEventId(liveMessageId);
            dm.setType(live.getType());
            dm.setPayload(live.getPayload());
            dm.setReason("SUCCESS");
            dm.setMovedAt(LocalDateTime.now());

            deadLetterRepository.save(dm);
            liveMessageRepository.delete(live);

            log.info("[Core] Evento {} (tipo: {}) consumido por todos los destinos. Eliminado de Live.", eventId, eventType);
        }
    }

    private boolean allModulesConsumedSuccessfully(Integer eventId, String eventType) {
        List<String> expectedModules = getExpectedConsumers(eventType);
        if (expectedModules.isEmpty()) {
            log.info("[Core] Evento {} no tiene módulos destino configurados, se considera consumido.", eventId);
            return true;
        }

        List<MessageConsumption> consumptions = consumptionRepository.findByEventId(eventId);
        Set<String> consumedModules = consumptions.stream()
                .map(mc -> mc.getModuleName().toLowerCase())
                .collect(Collectors.toSet());

        boolean allOk = consumedModules.containsAll(expectedModules);

        if (allOk) {
            log.info("[Core] Evento {} consumido por todos los módulos esperados: {}", eventId, expectedModules);
        } else {
            log.info("[Core] Evento {} aún pendiente de consumo por: {}",
                    eventId,
                    expectedModules.stream().filter(m -> !consumedModules.contains(m)).toList());
        }

        return allOk;
    }

    private List<String> getExpectedConsumers(String eventType) {
        String type = eventType.toLowerCase();

        if (type.contains("stock") || type.contains("producto") || type.contains("marca") || type.contains("categoría")) {
            return List.of("ventas", "analitica");
        }
        if (type.contains("pendiente")) {
            return List.of("inventario");
        }
        if (type.contains("confirmada")) {
            return List.of("inventario", "analitica");
        }
        if (type.contains("cancelada") || type.contains("rollback")) {
            return List.of("inventario");
        }
        if (type.contains("review") || type.contains("favorito") || type.contains("vista")) {
            return List.of("analitica");
        }
        if (type.contains("batch")) {
            return List.of("ventas", "analitica");
        }
        return List.of(); // por defecto, ninguno
    }

    private void moveMessageToRetry(Integer liveMessageId, String failedModule) {
        Optional<LiveMessage> liveOpt = liveMessageRepository.findById(liveMessageId);
        if (liveOpt.isEmpty()) {
            log.warn("[Core] No se encontró LiveMessage con ID {} para mover a Retry", liveMessageId);
            return;
        }

        LiveMessage live = liveOpt.get();

        RetryMessage retry = new RetryMessage();
        retry.setEventId(live.getEventId());
        retry.setOriginalLiveId(live.getId());
        retry.setType(live.getType());
        retry.setPayload(live.getPayload());
        retry.setAttempts(0); //es 0 xq si estaba en la cola de vivos quiere decir que nunca se reintento
        retry.setMaxAttempts(defaultMaxAttempts);
        retry.setTtlSeconds(600L); // 10 min. modificarlo para pruebas o expos
        retry.setCreatedAt(LocalDateTime.now());
        retry.setNextAttemptAt(LocalDateTime.now().plusMinutes(2)); // reintentar en 2 min
        retry.setConsumerModule(failedModule);

        retryMessageRepository.save(retry);
        liveMessageRepository.delete(live);

        log.info("[Core] Evento {} (tipo: {}) movido a Retry. Módulo fallido: {}",
                live.getEventId(), live.getType(), failedModule);
    }
}
