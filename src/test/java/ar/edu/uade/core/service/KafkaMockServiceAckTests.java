package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAckTests {

    // Repos
    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void handleAcknowledgement_NullAck_DoesNothing() {
        service.handleAcknowledgement(null);
        // No debe tocar repositorios
        verifyNoInteractions(liveMessageRepository, retryMessageRepository, deadLetterRepository, consumptionRepository, eventRepository);
    }

    @Test
    void handleAcknowledgement_BlankEventId_Ignores() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("  ");
        ack.setConsumer("mod");
        ack.setStatus("SUCCESS");
        service.handleAcknowledgement(ack);
        verifyNoInteractions(liveMessageRepository, retryMessageRepository, deadLetterRepository, consumptionRepository, eventRepository);
    }

    @Test
    void handleAcknowledgement_Failed_MovesLiveToRetry() {
        // live con eventId=10
        LiveMessage live = new LiveMessage();
        live.setId(3);
        live.setEventId(10);
        live.setType("producto actualizado");
        live.setPayload("{}");
        live.setTimestamp(LocalDateTime.now());
        live.setOriginModule("ventas");

        when(liveMessageRepository.findByEventId(10)).thenReturn(Optional.of(live));
        when(liveMessageRepository.findById(3)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("10");
        ack.setConsumer("inventario");
        ack.setStatus("FAILED");
        ack.setAttempts(0);

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(liveMessageRepository).delete(live);
        verifyNoInteractions(deadLetterRepository);
    }

    @Test
    void handleAcknowledgement_Success_AllExpectedModules_DeadLetterAndDeleteLive() {
        // Evento tipo "estado pendiente" -> consumidores esperados: [inventario]
        LiveMessage live = new LiveMessage();
        live.setId(7);
        live.setEventId(20);
        live.setType("estado pendiente");
        live.setPayload("{}");
        when(liveMessageRepository.findByEventId(20)).thenReturn(Optional.of(live));

        // Simulamos que ya estará consumido por el módulo que llega en el ACK
        MessageConsumption mc = new MessageConsumption();
        mc.setEventId(20);
        mc.setLiveMessageId(7);
        mc.setModuleName("inventario");
        when(consumptionRepository.findByEventId(20)).thenReturn(List.of(mc));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("20");
        ack.setConsumer("inventario");
        ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(live);
        verifyNoInteractions(retryMessageRepository);
    }

    @Test
    void handleAcknowledgement_Success_NotAllExpectedModules_NoDeadLetterNoDelete() {
        // Tipo con consumidores esperados [ventas, analitica] (ej: contiene "producto")
        LiveMessage live = new LiveMessage();
        live.setId(12);
        live.setEventId(3000);
        live.setType("producto actualizado");
        live.setPayload("{}");
        when(liveMessageRepository.findByEventId(3000)).thenReturn(Optional.of(live));

        // Solo uno consumido
        MessageConsumption prev = new MessageConsumption();
        prev.setEventId(3000); prev.setLiveMessageId(12); prev.setModuleName("ventas");
        when(consumptionRepository.findByEventId(3000)).thenReturn(List.of(prev));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("3000");
        ack.setConsumer("ventas");
        ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verifyNoInteractions(deadLetterRepository);
        verify(liveMessageRepository, never()).delete(any());
    }


    @Test
    void handleAcknowledgement_UnknownStatus_OnlyRecordsConsumption() {
        LiveMessage live = new LiveMessage();
        live.setId(13);
        live.setEventId(33);
        live.setType("batch productos");
        live.setPayload("{}");
        when(liveMessageRepository.findByEventId(33)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("33");
        ack.setConsumer("modA");
        ack.setStatus("OTHER");

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verifyNoInteractions(deadLetterRepository, retryMessageRepository);
        verify(liveMessageRepository, never()).delete(any());
    }
}
