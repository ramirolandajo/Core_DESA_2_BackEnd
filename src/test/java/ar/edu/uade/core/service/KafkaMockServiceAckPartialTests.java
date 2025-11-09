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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAckPartialTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void successAck_WithPendingConsumers_DoesNotMoveToDead() {
        LiveMessage live = new LiveMessage();
        live.setId(8); live.setEventId(80); live.setType("compra confirmada"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(80)).thenReturn(Optional.of(live));
        // Para "confirmada" se espera [inventario, analitica]; sólo uno consumió
        MessageConsumption c = new MessageConsumption(); c.setEventId(80); c.setModuleName("inventario");
        when(consumptionRepository.findByEventId(80)).thenReturn(List.of(c));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("80"); ack.setConsumer("inventario"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verifyNoInteractions(deadLetterRepository);
        verify(liveMessageRepository, never()).delete(any(LiveMessage.class));
    }
}

