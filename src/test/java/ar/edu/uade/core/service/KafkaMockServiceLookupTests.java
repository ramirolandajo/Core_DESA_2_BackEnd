package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceLookupTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void messageLookup_FoundInLive_ByEventId() {
        LiveMessage lm = new LiveMessage();
        lm.setId(1); lm.setEventId(100); lm.setType("T"); lm.setPayload("P"); lm.setOriginModule("ventas");
        when(liveMessageRepository.findByEventId(100)).thenReturn(Optional.of(lm));

        MessageLookupResult r = service.messageLookup(null, 100);
        assertEquals("LIVE", r.getLocation());
        assertEquals(1, r.getLiveId());
        assertEquals(100, r.getEventId());
        assertEquals("ventas", r.getOriginModule());
    }

    @Test
    void messageLookup_FoundInRetry_ByOriginalLiveId() {
        RetryMessage rm = new RetryMessage();
        rm.setId(2); rm.setEventId(200); rm.setType("T2"); rm.setPayload("P2");
        when(retryMessageRepository.findByOriginalLiveId(9)).thenReturn(Optional.of(rm));

        MessageLookupResult r = service.messageLookup(9, null);
        assertEquals("RETRY", r.getLocation());
        assertEquals(2, r.getRetryId());
        assertEquals(200, r.getEventId());
    }

    @Test
    void messageLookup_FoundInDead_ByEventId() {
        DeadLetterMessage dm = new DeadLetterMessage();
        dm.setEventId(400); dm.setType("T4"); dm.setPayload("P4");
        when(deadLetterRepository.findAll()).thenReturn(List.of(dm));

        MessageLookupResult r = service.messageLookup(null, 400);
        assertEquals("DEAD", r.getLocation());
        assertEquals(400, r.getEventId());
    }

    @Test
    void messageLookup_NotFound() {
        when(deadLetterRepository.findAll()).thenReturn(List.of());
        MessageLookupResult r = service.messageLookup(1, 2);
        assertEquals("NOT_FOUND", r.getLocation());
    }
}

