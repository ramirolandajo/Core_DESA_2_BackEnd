package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "sendMaxAttempts", 1);
        ReflectionTestUtils.setField(service, "sendBackoffMs", 1L);
        ReflectionTestUtils.setField(service, "sendTimeoutMs", 10L);
    }

    @Test
    void ingestEvent_ValidRequest_PersistsEventAndPublishes() {
        EventRequest req = new EventRequest();
        req.setType("ventas.creada");
        req.setOriginModule("ventas");
        req.setPayload(java.util.Map.of("id", 1));
        req.setTimestamp(LocalDateTime.now());

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(10);
            return e;
        });
        when(topicResolver.resolveTopic(anyString(), anyString())).thenReturn("topicV");
        // Devuelve un SendResult no nulo para evitar el fallo por reintentos
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sr = (SendResult<String, Object>) mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> okFuture = CompletableFuture.completedFuture(sr);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(okFuture);

        Event saved = service.ingestEvent(req);

        assertNotNull(saved);
        assertEquals(10, saved.getId());
        verify(eventRepository).save(any(Event.class));
        verify(liveMessageRepository).save(any(LiveMessage.class));
        verify(topicResolver).resolveTopic(eq("ventas.creada"), eq("ventas"));
        verify(kafkaTemplate).send(eq("topicV"), anyString(), any());
    }

    @Test
    void ingestEvent_NullRequest_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> service.ingestEvent(null));
    }

    @Test
    void ingestEvent_BlankType_ThrowsIAE() {
        EventRequest req = new EventRequest();
        req.setType(" ");
        req.setOriginModule("ventas");
        assertThrows(IllegalArgumentException.class, () -> service.ingestEvent(req));
    }

    @Test
    void ingestEvent_BlankOrigin_ThrowsIAE() {
        EventRequest req = new EventRequest();
        req.setType("ventas.creada");
        req.setOriginModule(" ");
        assertThrows(IllegalArgumentException.class, () -> service.ingestEvent(req));
    }

    @Test
    void consumeOneAndMoveToRetry_NotFound_ReturnsNotFound() {
        when(liveMessageRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(liveMessageRepository.findByEventId(anyInt())).thenReturn(Optional.empty());
        when(retryMessageRepository.findByOriginalLiveId(anyInt())).thenReturn(Optional.empty());
        when(retryMessageRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(retryMessageRepository.findByEventId(anyInt())).thenReturn(Optional.empty());

        ConsumeResult res = service.consumeOneAndMoveToRetry(99, 100, "modX");
        assertEquals(ConsumeResult.Status.NOT_FOUND, res.getStatus());
    }

    @Test
    void consumeOneAndMoveToRetry_LiveFoundSameOrigin_Conflict() {
        LiveMessage live = new LiveMessage();
        live.setId(1);
        live.setEventId(7);
        live.setOriginModule("ventas");
        when(liveMessageRepository.findById(1)).thenReturn(Optional.of(live));

        ConsumeResult res = service.consumeOneAndMoveToRetry(1, null, "VENTAS");
        assertEquals(ConsumeResult.Status.CONFLICT, res.getStatus());
    }

    @Test
    void consumeOneAndMoveToRetry_LiveFound_FirstConsumptionCreatesRetry() {
        LiveMessage live = new LiveMessage();
        live.setId(1);
        live.setEventId(7);
        live.setType("ventas.creada");
        live.setPayload("{}");
        when(liveMessageRepository.findById(1)).thenReturn(Optional.of(live));
        when(consumptionRepository.existsByEventIdAndModuleName(eq(7), anyString())).thenReturn(false);
        when(consumptionRepository.findByEventId(7)).thenReturn(java.util.Collections.emptyList());

        ConsumeResult res = service.consumeOneAndMoveToRetry(1, null, "inventario");

        assertEquals(ConsumeResult.Status.RETRY, res.getStatus());
        assertNotNull(res.getRetryMessage());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(liveMessageRepository).deleteById(1);
    }

    @Test
    void failRetry_WhenBelowMaxAttempts_ReturnsRetryAndIncrements() {
        RetryMessage rm = new RetryMessage();
        rm.setId(5);
        rm.setEventId(33);
        rm.setAttempts(0);
        rm.setMaxAttempts(2);
        when(retryMessageRepository.findById(5)).thenReturn(Optional.of(rm));

        ConsumeResult res = service.failRetry(5);

        assertEquals(ConsumeResult.Status.RETRY, res.getStatus());
        assertEquals(1, res.getRetryMessage().getAttempts());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(deadLetterRepository, never()).save(any());
    }

    @Test
    void failRetry_WhenReachesMaxAttempts_MovesToDead() {
        RetryMessage rm = new RetryMessage();
        rm.setId(5);
        rm.setEventId(33);
        rm.setAttempts(1);
        rm.setMaxAttempts(2);
        rm.setType("ventas");
        rm.setPayload("{}");
        when(retryMessageRepository.findById(5)).thenReturn(Optional.of(rm));

        ConsumeResult res = service.failRetry(5);

        assertEquals(ConsumeResult.Status.DEAD, res.getStatus());
        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(5);
    }

    @Test
    void processRetriesAndExpire_ExpiresByTtlOrAttempts() {
        RetryMessage a = new RetryMessage();
        a.setId(1); a.setEventId(1); a.setAttempts(99); a.setMaxAttempts(3); a.setPayload("{}"); a.setType("t");
        RetryMessage b = new RetryMessage();
        b.setId(2); b.setEventId(2); b.setAttempts(0); b.setMaxAttempts(9); b.setPayload("{}"); b.setType("t");
        b.setCreatedAt(LocalDateTime.now().minusHours(2)); b.setTtlSeconds(60L);
        when(retryMessageRepository.findAll()).thenReturn(List.of(a,b));

        service.processRetriesAndExpire();

        verify(deadLetterRepository, times(2)).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(1);
        verify(retryMessageRepository).deleteById(2);
    }

    @Test
    void handleAcknowledgement_NullAck_IsNoop() {
        service.handleAcknowledgement(null);
        verifyNoInteractions(liveMessageRepository, retryMessageRepository, deadLetterRepository, consumptionRepository);
    }

    @Test
    void handleAcknowledgement_UnknownStatus_NoActionButConsumptionStoredWhenLiveFound() {
        LiveMessage live = new LiveMessage();
        live.setId(3);
        live.setEventId(33);
        live.setType("ventas.creada");
        when(liveMessageRepository.findByEventId(33)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("33"); // numérico para mapear a live.eventId
        ack.setConsumer("modA");
        ack.setStatus("OTHER");

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(liveMessageRepository, never()).delete(any());
        verify(retryMessageRepository, never()).save(any());
    }
}
