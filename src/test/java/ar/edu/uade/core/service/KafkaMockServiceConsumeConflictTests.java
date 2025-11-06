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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceConsumeConflictTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private EventRepository eventRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void consumeFromLive_SameOrigin_Conflict() {
        LiveMessage live = new LiveMessage();
        live.setId(100); live.setEventId(1000); live.setType("x"); live.setOriginModule("ventas");
        when(liveMessageRepository.findByEventId(1000)).thenReturn(Optional.of(live));

        ConsumeResult res = service.consumeOneAndMoveToRetry(null, 1000, "ventas");
        assertEquals(ConsumeResult.Status.CONFLICT, res.getStatus());
        verifyNoInteractions(retryMessageRepository);
    }

    @Test
    void consumeFromLive_AlreadyConsumed_Conflict() {
        LiveMessage live = new LiveMessage();
        live.setId(101); live.setEventId(1001); live.setType("x"); live.setOriginModule("core");
        when(liveMessageRepository.findByEventId(1001)).thenReturn(Optional.of(live));
        when(consumptionRepository.existsByEventIdAndModuleName(1001, "modX")).thenReturn(true);

        ConsumeResult res = service.consumeOneAndMoveToRetry(null, 1001, "modX");
        assertEquals(ConsumeResult.Status.CONFLICT, res.getStatus());
        verifyNoInteractions(retryMessageRepository);
    }

    @Test
    void failRetry_NotFound_ReturnsNotFound() {
        when(retryMessageRepository.findById(999)).thenReturn(Optional.empty());
        ConsumeResult res = service.failRetry(999);
        assertEquals(ConsumeResult.Status.NOT_FOUND, res.getStatus());
    }
}

