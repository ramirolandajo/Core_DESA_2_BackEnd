package ar.edu.uade.core.service;

import ar.edu.uade.core.model.ConsumeResult;
import ar.edu.uade.core.model.DeadLetterMessage;
import ar.edu.uade.core.model.RetryMessage;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceBasicOpsTests {

    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void acknowledgeRetry_Exists_DeletesAndReturnsTrue() {
        when(retryMessageRepository.existsById(123)).thenReturn(true);
        assertTrue(service.acknowledgeRetry(123));
        verify(retryMessageRepository).deleteById(123);
    }

    @Test
    void failRetry_WhenNotFound_ReturnsNotFound() {
        when(retryMessageRepository.findById(55)).thenReturn(Optional.empty());
        ConsumeResult r = service.failRetry(55);
        assertEquals(ConsumeResult.Status.NOT_FOUND, r.getStatus());
        verifyNoInteractions(deadLetterRepository);
    }

    @Test
    void failRetry_WhenExceedsMax_MovesToDeadAndDeletes() {
        RetryMessage rm = new RetryMessage();
        rm.setId(9); rm.setEventId(900); rm.setType("T"); rm.setPayload("P");
        rm.setAttempts(2); rm.setMaxAttempts(2);
        when(retryMessageRepository.findById(9)).thenReturn(Optional.of(rm));

        ConsumeResult r = service.failRetry(9);
        assertEquals(ConsumeResult.Status.DEAD, r.getStatus());
        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(9);
    }
}

