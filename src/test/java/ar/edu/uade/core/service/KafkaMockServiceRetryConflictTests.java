package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceRetryConflictTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void consumeOneAndMoveToRetry_FromRetry_SameOrigin_Conflict() {
        // No hay live
        when(liveMessageRepository.findByEventId(anyInt())).thenReturn(Optional.empty());

        // Hay retry por eventId 42
        RetryMessage rm = new RetryMessage();
        rm.setId(7); rm.setEventId(42); rm.setType("T"); rm.setPayload("{}"); rm.setAttempts(0); rm.setMaxAttempts(3);
        when(retryMessageRepository.findByEventId(42)).thenReturn(Optional.of(rm));

        // El evento original tiene originModule "ventas"
        Event ev = new Event(); ev.setId(42); ev.setOriginModule("ventas");
        when(eventRepository.findById(42)).thenReturn(Optional.of(ev));

        // Consumidor intenta con el mismo módulo "ventas" -> debe resultar en CONFLICT
        ConsumeResult cr = service.consumeOneAndMoveToRetry(null, 42, "ventas");
        assertEquals(ConsumeResult.Status.CONFLICT, cr.getStatus());
        assertTrue(cr.getMessage().toLowerCase().contains("consumer module"));
        verify(consumptionRepository, never()).save(any());
        verify(retryMessageRepository, never()).save(any());
    }
}
