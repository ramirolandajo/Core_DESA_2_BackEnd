package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAckMoreEdgeTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void handleAcknowledgement_BlankEventId_ShouldIgnore() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId(" \t\n");
        ack.setConsumer("mod");
        ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verifyNoInteractions(liveMessageRepository, retryMessageRepository, deadLetterRepository, consumptionRepository);
    }

    @Test
    void handleAcknowledgement_NullConsumer_RegistersAsDesconocido() {
        LiveMessage live = new LiveMessage();
        live.setId(1); live.setEventId(10); live.setType("ventas.creada");
        when(liveMessageRepository.findByEventId(10)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("10");
        ack.setConsumer(null);
        ack.setStatus("OTHER");

        service.handleAcknowledgement(ack);

        ArgumentCaptor<MessageConsumption> cap = ArgumentCaptor.forClass(MessageConsumption.class);
        verify(consumptionRepository).save(cap.capture());
        assertEquals("Desconocido", cap.getValue().getModuleName());
        // No movimientos
        verify(liveMessageRepository, never()).delete(any());
        verify(retryMessageRepository, never()).save(any());
        verify(deadLetterRepository, never()).save(any());
    }

    @Test
    void handleAcknowledgement_Success_WithCategoriaAccent_AllConsumedMovesToDead() {
        LiveMessage live = new LiveMessage();
        live.setId(3); live.setEventId(33); live.setType("categoría actualizada"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(33)).thenReturn(Optional.of(live));
        // Para tipos que contienen "categoría" => se esperan [ventas, analitica]
        MessageConsumption c1 = new MessageConsumption(); c1.setEventId(33); c1.setModuleName("ventas");
        MessageConsumption c2 = new MessageConsumption(); c2.setEventId(33); c2.setModuleName("analitica");
        when(consumptionRepository.findByEventId(33)).thenReturn(List.of(c1, c2));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("33"); ack.setConsumer("ventas"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }
}
