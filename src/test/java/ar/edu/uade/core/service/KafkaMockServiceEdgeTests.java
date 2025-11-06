package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceEdgeTests {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private LiveMessageRepository liveMessageRepository;
    @Mock
    private RetryMessageRepository retryMessageRepository;
    @Mock
    private DeadLetterRepository deadLetterRepository;
    @Mock
    private MessageConsumptionRepository consumptionRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TopicResolver topicResolver;

    @InjectMocks
    private KafkaMockService service;

    @Test
    void acknowledgeRetry_WhenNotExists_ReturnsFalse() {
        when(retryMessageRepository.existsById(1)).thenReturn(false);
        assertFalse(service.acknowledgeRetry(1));
    }


    @Test
    void failRetry_WhenNotFound_ReturnsNotFound() {
        when(retryMessageRepository.findById(123)).thenReturn(Optional.empty());
        ConsumeResult res = service.failRetry(123);
        assertEquals(ConsumeResult.Status.NOT_FOUND, res.getStatus());
    }

    @Test
    void processRetriesAndExpire_WhenWithinLimits_DoesNothing() {
        RetryMessage rm = new RetryMessage();
        rm.setId(9);
        rm.setEventId(90);
        rm.setType("T");
        rm.setPayload("{}");
        rm.setAttempts(0);
        rm.setMaxAttempts(3);
        rm.setCreatedAt(java.time.LocalDateTime.now());
        rm.setTtlSeconds(3600L);

        when(retryMessageRepository.findAll()).thenReturn(List.of(rm));

        service.processRetriesAndExpire();

        verify(deadLetterRepository, never()).save(any());
        verify(retryMessageRepository, never()).deleteById(any());
    }
}
