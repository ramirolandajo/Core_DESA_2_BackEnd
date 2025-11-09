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
class KafkaMockServiceMiscSmallTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void messageLookup_WhenNothingFound_ReturnsNotFound() {
        MessageLookupResult lr = service.messageLookup(null, null);
        assertEquals("NOT_FOUND", lr.getLocation());
    }

    @Test
    void handleAcknowledgement_Success_TypeBatch_AllModules_DeadAndDeleted() {
        LiveMessage live = new LiveMessage();
        live.setId(5); live.setEventId(500); live.setType("batch productos"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(500)).thenReturn(Optional.of(live));
        // para "batch" se esperan [ventas, analitica] y simulamos que ambos ya consumieron
        MessageConsumption c1 = new MessageConsumption(); c1.setEventId(500); c1.setModuleName("ventas");
        MessageConsumption c2 = new MessageConsumption(); c2.setEventId(500); c2.setModuleName("analitica");
        when(consumptionRepository.findByEventId(500)).thenReturn(List.of(c1,c2));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("500"); ack.setConsumer("ventas"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }
}

