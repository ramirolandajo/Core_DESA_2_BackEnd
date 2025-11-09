package ar.edu.uade.core.service;

import ar.edu.uade.core.model.DeadLetterMessage;
import ar.edu.uade.core.model.RetryMessage;
import ar.edu.uade.core.repository.DeadLetterRepository;
import ar.edu.uade.core.repository.RetryMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceRetriesProcessTests {

    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void processRetries_NoMove_WhenAttemptsBelowMaxAndNotExpired() {
        RetryMessage r = new RetryMessage();
        r.setId(1); r.setEventId(1); r.setType("x"); r.setPayload("{}");
        r.setAttempts(1); r.setMaxAttempts(5);
        r.setTtlSeconds(3600L); r.setCreatedAt(LocalDateTime.now());
        when(retryMessageRepository.findAll()).thenReturn(List.of(r));

        service.processRetriesAndExpire();

        verifyNoInteractions(deadLetterRepository);
        verify(retryMessageRepository, never()).deleteById(anyInt());
    }
}

