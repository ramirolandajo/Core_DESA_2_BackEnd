package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAcknowledgeTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private RetryMessageRepository retryMessageRepository;

    @InjectMocks private KafkaMockService service;

    @BeforeEach
    void relaxSendFields() throws Exception {
        setPrivate(service, "sendMaxAttempts", 1);
        setPrivate(service, "sendBackoffMs", 0L);
        setPrivate(service, "sendTimeoutMs", 100L);
    }

    private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void handleAcknowledgement_NullAck_NoInteractions() {
        service.handleAcknowledgement(null);
        verifyNoInteractions(liveMessageRepository, deadLetterRepository, consumptionRepository);
    }

    @Test
    void handleAcknowledgement_InvalidEventId_Ignored() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("");
        service.handleAcknowledgement(ack);
        verifyNoInteractions(liveMessageRepository, deadLetterRepository, consumptionRepository);
    }

    @Test
    void handleAcknowledgement_Success_AllModules_MovesToDeadAndDeleteLive() {
        LiveMessage live = new LiveMessage();
        live.setId(9); live.setEventId(90); live.setType("estado pendiente"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(90)).thenReturn(Optional.of(live));
        // Para "pendiente" se espera [inventario]
        MessageConsumption c = new MessageConsumption(); c.setEventId(90); c.setModuleName("inventario");
        when(consumptionRepository.findByEventId(90)).thenReturn(List.of(c));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("90"); ack.setConsumer("inventario"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }

    @Test
    void handleAcknowledgement_UnknownStatus_NoAction() {
        LiveMessage live = new LiveMessage();
        live.setId(7); live.setEventId(70); live.setType("batch productos"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(70)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("70"); ack.setConsumer("modA"); ack.setStatus("OTHER");

        service.handleAcknowledgement(ack);

        verifyNoInteractions(deadLetterRepository, retryMessageRepository);
        verify(consumptionRepository).save(any(MessageConsumption.class));
    }
}
