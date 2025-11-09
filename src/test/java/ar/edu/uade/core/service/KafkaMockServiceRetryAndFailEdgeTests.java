package ar.edu.uade.core.service;

import ar.edu.uade.core.model.ConsumeResult;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceRetryAndFailEdgeTests {

    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void acknowledgeRetry_NotExists_ReturnsFalse() {
        when(retryMessageRepository.existsById(123)).thenReturn(false);
        assertFalse(service.acknowledgeRetry(123));
        verify(retryMessageRepository, never()).deleteById(anyInt());
    }

    @Test
    void failRetry_UnderMax_IncrementsAndReturnsRetry() {
        RetryMessage rm = new RetryMessage();
        rm.setId(2); rm.setEventId(200); rm.setType("x"); rm.setPayload("{}");
        rm.setAttempts(0); rm.setMaxAttempts(3);
        when(retryMessageRepository.findById(2)).thenReturn(Optional.of(rm));

        ConsumeResult res = service.failRetry(2);
        assertEquals(ConsumeResult.Status.RETRY, res.getStatus());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verifyNoInteractions(deadLetterRepository);
    }

    @Test
    void processRetries_AttemptsExceeded_MovesToDead() {
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
    void consumeOne_NotFound_ReturnsNotFound() {
        // no live, no retry, no fallback
        assertEquals(ConsumeResult.Status.NOT_FOUND, service.consumeOneAndMoveToRetry(null, null, "m").getStatus());
    }
}
