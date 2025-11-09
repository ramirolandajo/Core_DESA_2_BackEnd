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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceMoreTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks
    private KafkaMockService service;

    @BeforeEach
    void tuneSendConfig() {
        // Evitar delays/reintentos largos en pruebas
        ReflectionTestUtils.setField(service, "sendMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "sendBackoffMs", 1L);
        ReflectionTestUtils.setField(service, "sendTimeoutMs", 50L);
    }

    static class SelfRefPayload { SelfRefPayload self = this; }

    @Test
    void ingestEvent_ShouldThrowWhenOriginBlank() {
        EventRequest req = new EventRequest();
        req.setType("X");
        req.setOriginModule(" ");
        assertThrows(IllegalArgumentException.class, () -> service.ingestEvent(req));
    }

    @Test
    void ingestEvent_WhenPayloadSerializationFails_ThrowsRuntimeException() {
        EventRequest req = new EventRequest();
        req.setType("TEST");
        req.setOriginModule("ventas");
        req.setPayload(new SelfRefPayload()); // Jackson fallará por referencia cíclica

        assertThrows(RuntimeException.class, () -> service.ingestEvent(req));
    }

    @Test
    void ingestEvent_SendToKafkaRetriesAndEventuallySucceeds() {
        EventRequest req = new EventRequest();
        req.setType("ventas.created");
        req.setOriginModule("ventas");
        req.setPayload("ok");

        Event ev = new Event(); ev.setId(1); ev.setType(req.getType()); ev.setPayload("ok"); ev.setOriginModule("ventas");
        when(eventRepository.save(any(Event.class))).thenReturn(ev);
        when(topicResolver.resolveTopic(anyString(), anyString())).thenReturn("ventas-topic");

        // Primer intento falla (future excepcional), segundo intento éxito
        CompletableFuture futureFail = new CompletableFuture<>();
        futureFail.completeExceptionally(new RuntimeException("boom"));
        CompletableFuture futureOk = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(futureFail)
                .thenReturn(futureOk);

        Event result = service.ingestEvent(req);
        assertNotNull(result);
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        verify(liveMessageRepository).save(any(LiveMessage.class));
    }

    @Test
    void consumeOneAndMoveToRetry_ConflictWhenConsumerEqualsOrigin() {
        LiveMessage lm = new LiveMessage();
        lm.setId(10); lm.setEventId(100); lm.setOriginModule("ventas");
        when(liveMessageRepository.findById(10)).thenReturn(Optional.of(lm));

        ConsumeResult r = service.consumeOneAndMoveToRetry(10, null, "ventas");
        assertEquals(ConsumeResult.Status.CONFLICT, r.getStatus());
        assertTrue(r.getMessage().toLowerCase().contains("consumer module"));
        verify(retryMessageRepository, never()).save(any());
    }

    @Test
    void failRetry_WhenBelowMaxAttempts_ReturnsRetryAndUpdates() {
        RetryMessage rm = new RetryMessage();
        rm.setId(9); rm.setEventId(90); rm.setType("T"); rm.setPayload("{}");
        rm.setAttempts(0); rm.setMaxAttempts(5);
        when(retryMessageRepository.findById(9)).thenReturn(Optional.of(rm));

        ConsumeResult r = service.failRetry(9);
        assertEquals(ConsumeResult.Status.RETRY, r.getStatus());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(deadLetterRepository, never()).save(any());
    }


    @Test
    void getConsumptions_WhenNoIds_ReturnsAll() {
        when(consumptionRepository.findAll()).thenReturn(List.of(new MessageConsumption()));
        assertEquals(1, service.getConsumptions(null, null).size());
    }

    @Test
    void handleAcknowledgement_SuccessButNotAllConsumers_KeepsLive() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("1000"); ack.setConsumer("ventas"); ack.setStatus("SUCCESS");
        LiveMessage lm = new LiveMessage(); lm.setId(10); lm.setEventId(1000); lm.setType("compra confirmada");
        when(liveMessageRepository.findByEventId(1000)).thenReturn(Optional.of(lm));
        // eventType contiene "confirmada" => se esperan [inventario, analitica]
        // Solo consumió "ventas" (no esperada), igual se registra consumo, pero no se moverá ni borrará
        when(consumptionRepository.findByEventId(1000)).thenReturn(List.of());

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(liveMessageRepository, never()).delete(any(LiveMessage.class));
        verify(deadLetterRepository, never()).save(any());
        verify(retryMessageRepository, never()).save(any());
    }

    @Test
    void handleAcknowledgement_UnknownStatus_OnlyRegistersConsumption() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("2000"); ack.setConsumer("ventas"); ack.setStatus("WEIRD");
        LiveMessage lm = new LiveMessage(); lm.setId(20); lm.setEventId(2000); lm.setType("producto actualizado");
        when(liveMessageRepository.findByEventId(2000)).thenReturn(Optional.of(lm));

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(liveMessageRepository, never()).delete(any(LiveMessage.class));
        verify(retryMessageRepository, never()).save(any());
        verify(deadLetterRepository, never()).save(any());
    }
}

