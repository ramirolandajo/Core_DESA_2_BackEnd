package ar.edu.uade.core.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.uade.core.mock.MockDataFactory;
import ar.edu.uade.core.model.ConsumeResult;
import ar.edu.uade.core.model.DeadLetterMessage;
import ar.edu.uade.core.model.Event;
import ar.edu.uade.core.model.LiveMessage;
import ar.edu.uade.core.model.MessageConsumption;
import ar.edu.uade.core.model.RetryMessage;
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

    private final MockDataFactory mock = new MockDataFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int defaultMaxAttempts = 3;
    private final long defaultRetryDelaySeconds = 30;
    private final long defaultTtlSeconds = 60 * 60;
    private final int distinctConsumptionThreshold = 2;

    // ----------------- event & live creation (mocks) -----------------
    private Event persistEvent(String type, Object payload) {
        try {
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                payloadJson = payload.toString();
            }
            String origin = inferOriginFromType(type);
            Event event = new Event(type, payloadJson, origin);
            return eventRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando payload", e);
        }
    }

    private String inferOriginFromType(String type) {
        if (type == null) return "MOCK";
        String t = type.toLowerCase();
        if (t.contains("cart") || t.contains("compra") || t.contains("purchase") || t.contains("cartpurchase")) return "Ventas";
        if (t.contains("stock") || t.contains("reserve") || t.contains("inventory") || t.contains("stockreserved")) return "Inventario";
        if (t.contains("view") || t.contains("daily") || t.contains("favourite") || t.contains("fav") || t.contains("analytics") || t.contains("product_views") ) return "Analitica";
        return "MOCK";
    }

    private Event sendEvent(String type, Object payload) {
        Event event = persistEvent(type, payload);
        LiveMessage lm = new LiveMessage();
        lm.setEventId(event.getId());
        lm.setType(event.getType());
        lm.setPayload(event.getPayload());
        lm.setTimestamp(event.getTimestamp());
        lm.setOriginModule(event.getOriginModule());
        liveMessageRepository.save(lm);
        return event;
    }

    public List<Event> getAll(){
        return eventRepository.findAll();
    }

    // expose some mocks
    public List<Event> createBrand() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("POST: Marca creada", MockDataFactory.sony()));
        results.add(sendEvent("POST: Marca creada", MockDataFactory.apple()));
        return results;
    }

    // RESTAURADOS: métodos que llenan la tabla event + live_message
    public List<Event> createCategory() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("POST: Categoría creada", MockDataFactory.audio()));
        results.add(sendEvent("POST: Categoría creada", MockDataFactory.wearables()));
        return results;
    }

    public List<Event> deactivateCategory() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PATCH: Categoría desactivada", MockDataFactory.audioInactive()));
        return results;
    }

    public List<Event> createProduct() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("POST: Producto creado", MockDataFactory.sonyHeadphones()));
        return results;
    }

    public List<Event> updateProductStockDecrease() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PATCH: Stock disminuido", MockDataFactory.sonyHeadphonesDecreaseStock()));
        return results;
    }

    public List<Event> updateProductStockIncrease() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PATCH: Stock aumentado", MockDataFactory.sonyHeadphonesIncreaseStock()));
        return results;
    }

    public List<Event> updateProductPrice() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PATCH: Precio actualizado", MockDataFactory.sonyHeadphonesPriceChange()));
        return results;
    }

    public List<Event> updateProductGeneral() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PUT: Producto actualizado", MockDataFactory.sonyHeadphonesUpdated()));
        return results;
    }

    public List<Event> deactivateProduct() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PATCH: Producto desactivado", MockDataFactory.sonyHeadphonesDeactivate()));
        return results;
    }

    public List<Event> createCart() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("POST: Carrito creado", MockDataFactory.cartWithSony()));
        return results;
    }

    public List<Event> updateCartAddProduct() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PUT: Producto agregado al carrito", MockDataFactory.cartAddAppleWatch()));
        return results;
    }

    public List<Event> updateCartRemoveProduct() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("PUT: Producto eliminado del carrito", MockDataFactory.cartRemoveSony()));
        return results;
    }

    public List<Event> createPurchase() {
        List<Event> results = new ArrayList<>();
        results.add(sendEvent("POST: Compra pendiente", MockDataFactory.purchasePending()));
        results.add(sendEvent("PATCH: Compra confirmada", MockDataFactory.purchaseConfirmed()));
        results.add(sendEvent("PATCH: Compra enviada", MockDataFactory.purchaseShipped()));
        return results;
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
        if (rm == null && liveId != null) rm = retryMessageRepository.findById(liveId).orElse(null);
        if (rm == null && eventId != null) rm = retryMessageRepository.findByEventId(eventId).orElse(null);
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

    // Obtener live/retry por id
    public LiveMessage getLiveById(Integer id){
        return liveMessageRepository.findById(id).orElse(null);
    }

    public RetryMessage getRetryById(Integer id){
        return retryMessageRepository.findById(id).orElse(null);
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

}
