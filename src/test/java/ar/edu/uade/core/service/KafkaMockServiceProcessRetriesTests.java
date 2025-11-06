package ar.edu.uade.core.service;

import ar.edu.uade.core.model.DeadLetterMessage;
import ar.edu.uade.core.model.RetryMessage;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceProcessRetriesTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void processRetriesAndExpire_WhenAttemptsExceeded_MovesToDead() {
        RetryMessage r = new RetryMessage();
        r.setId(9); r.setEventId(900); r.setType("x"); r.setPayload("{}");
        r.setAttempts(5); r.setMaxAttempts(3);
        r.setCreatedAt(LocalDateTime.now().minusMinutes(1)); r.setTtlSeconds(3600L);
        when(retryMessageRepository.findAll()).thenReturn(List.of(r));

        service.processRetriesAndExpire();

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(9);
    }

    @Test
    void processRetriesAndExpire_WhenTtlExpired_MovesToDead() {
        RetryMessage r = new RetryMessage();
        r.setId(10); r.setEventId(901); r.setType("x"); r.setPayload("{}");
        r.setAttempts(0); r.setMaxAttempts(5);
        r.setCreatedAt(LocalDateTime.now().minusHours(2)); r.setTtlSeconds(60L); // TTL vencido
        when(retryMessageRepository.findAll()).thenReturn(List.of(r));

        service.processRetriesAndExpire();

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(10);
    }

    @Test
    void processRetriesAndExpire_WhenNoneExceeded_NoAction() {
        RetryMessage r = new RetryMessage();
        r.setId(11); r.setEventId(902); r.setType("x"); r.setPayload("{}");
        r.setAttempts(0); r.setMaxAttempts(5);
        r.setCreatedAt(LocalDateTime.now()); r.setTtlSeconds(3600L);
        when(retryMessageRepository.findAll()).thenReturn(List.of(r));

        service.processRetriesAndExpire();

        verifyNoInteractions(deadLetterRepository);
        verify(retryMessageRepository, never()).deleteById(anyInt());
    }
}

