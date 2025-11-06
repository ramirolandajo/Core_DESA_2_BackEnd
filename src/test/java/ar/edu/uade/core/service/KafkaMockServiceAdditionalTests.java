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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAdditionalTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @BeforeEach
    void tune() {
        ReflectionTestUtils.setField(service, "sendMaxAttempts", 1);
        ReflectionTestUtils.setField(service, "sendBackoffMs", 1L);
        ReflectionTestUtils.setField(service, "sendTimeoutMs", 10L);
    }

    @Test
    void ingestEvent_WhenKafkaSendPermanentlyFails_ThrowsIllegalState() {
        EventRequest req = new EventRequest();
        req.setType("ventas.created");
        req.setOriginModule("ventas");
        req.setPayload("p");

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> { Event e = inv.getArgument(0); e.setId(1); return e;});
        when(topicResolver.resolveTopic(anyString(), anyString())).thenReturn("ventas-topic");
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> fail = new CompletableFuture<>();
        fail.completeExceptionally(new RuntimeException("boom"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(fail);

        assertThrows(IllegalStateException.class, () -> service.ingestEvent(req));
    }

    @Test
    void consumeOneAndMoveToRetry_WhenAlreadyConsumed_Conflict() {
        LiveMessage live = new LiveMessage();
        live.setId(2); live.setEventId(20); live.setType("T"); live.setPayload("{}"); live.setOriginModule("ventas");
        when(liveMessageRepository.findById(2)).thenReturn(Optional.of(live));
        when(consumptionRepository.existsByEventIdAndModuleName(20, "modA")).thenReturn(true);

        ConsumeResult res = service.consumeOneAndMoveToRetry(2, null, "modA");
        assertEquals(ConsumeResult.Status.CONFLICT, res.getStatus());
        verify(retryMessageRepository, never()).save(any());
    }

    @Test
    void handleAcknowledgement_Success_AllModulesConsumed_MovesToDead() {
        LiveMessage live = new LiveMessage();
        live.setId(8); live.setEventId(88); live.setType("compra confirmada"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(88)).thenReturn(Optional.of(live));
        // Para type que contiene "confirmada" se esperan [inventario, analitica]
        MessageConsumption c1 = new MessageConsumption(); c1.setEventId(88); c1.setModuleName("inventario");
        MessageConsumption c2 = new MessageConsumption(); c2.setEventId(88); c2.setModuleName("analitica");
        when(consumptionRepository.findByEventId(88)).thenReturn(List.of(c1, c2));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("88"); ack.setConsumer("inventario"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }
}
