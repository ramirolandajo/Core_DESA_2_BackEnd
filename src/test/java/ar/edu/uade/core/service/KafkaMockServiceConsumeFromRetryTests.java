package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceConsumeFromRetryTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void consumeOne_FromRetry_SameOrigin_ReturnsConflict() {
        RetryMessage rm = new RetryMessage();
        rm.setId(5); rm.setEventId(500); rm.setType("T"); rm.setPayload("P");
        when(retryMessageRepository.findById(5)).thenReturn(Optional.of(rm));
        // origin del evento = consumer, provoca conflicto
        Event ev = new Event();
        ev.setId(500); ev.setOriginModule("m");
        when(eventRepository.findById(500)).thenReturn(Optional.of(ev));

        ConsumeResult r = service.consumeOneAndMoveToRetry(5, null, "m");
        assertEquals(ConsumeResult.Status.CONFLICT, r.getStatus());
        verify(consumptionRepository, never()).save(any());
    }

    @Test
    void consumeOne_FromRetry_AlreadyConsumedByModule_ReturnsConflict() {
        RetryMessage rm = new RetryMessage();
        rm.setId(6); rm.setEventId(600); rm.setType("T"); rm.setPayload("P");
        when(retryMessageRepository.findById(6)).thenReturn(Optional.of(rm));
        when(eventRepository.findById(600)).thenReturn(Optional.of(new Event()));
        when(consumptionRepository.existsByEventIdAndModuleName(600, "modX")).thenReturn(true);

        ConsumeResult r = service.consumeOneAndMoveToRetry(6, null, "modX");
        assertEquals(ConsumeResult.Status.CONFLICT, r.getStatus());
    }

    @Test
    void consumeOne_FromRetry_NewConsumer_RecordsConsumptionAndIncrementsAttempts() {
        RetryMessage rm = new RetryMessage();
        rm.setId(7); rm.setEventId(700); rm.setType("T"); rm.setPayload("P"); rm.setAttempts(0); rm.setMaxAttempts(3);
        when(retryMessageRepository.findById(7)).thenReturn(Optional.of(rm));
        when(eventRepository.findById(700)).thenReturn(Optional.of(new Event()));
        when(consumptionRepository.findByEventId(700)).thenReturn(List.of());

        ConsumeResult r = service.consumeOneAndMoveToRetry(7, null, "modY");

        assertEquals(ConsumeResult.Status.RETRY, r.getStatus());
        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(retryMessageRepository).save(argThat(x -> x.getAttempts() != null && x.getAttempts() == 1));
        verifyNoInteractions(deadLetterRepository);
    }

}

