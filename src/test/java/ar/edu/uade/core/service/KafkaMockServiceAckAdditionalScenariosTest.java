package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAckAdditionalScenariosTest {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;

    @InjectMocks private KafkaMockService service;

    @Test
    void handleAcknowledgement_Success_TypePendiente_NoExpectedModules_TreatedAsConsumed() {
        // eventType contiene "pendiente" => expected [inventario]
        LiveMessage live = new LiveMessage();
        live.setId(1); live.setEventId(10); live.setType("estado pendiente"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(10)).thenReturn(Optional.of(live));
        // Consumptions contiene el esperado (inventario)
        MessageConsumption c = new MessageConsumption(); c.setEventId(10); c.setModuleName("inventario");
        when(consumptionRepository.findByEventId(10)).thenReturn(List.of(c));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("10"); ack.setConsumer("inventario"); ack.setStatus("SUCCESS");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }

    @Test
    void handleAcknowledgement_Success_WithNoExpectedConsumers_DefaultType_MarksAsDoneImmediately() {
        LiveMessage live = new LiveMessage();
        live.setId(2); live.setEventId(20); live.setType("evento sin destino");
        when(liveMessageRepository.findByEventId(20)).thenReturn(Optional.of(live));
        // Para tipos no reconocidos, getExpectedConsumers devuelve lista vacía -> se considera consumido
        // No es necesario stubear consumptionRepository.findByEventId cuando no hay consumidores esperados

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("20"); ack.setConsumer("mod"); ack.setStatus("CONSUMED");

        service.handleAcknowledgement(ack);

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
        verify(consumptionRepository, atLeastOnce()).save(any(MessageConsumption.class));
    }

    @Test
    void handleAcknowledgement_Fail_WithNullConsumer_RegistersUnknownAndMovesToRetry() {
        LiveMessage live = new LiveMessage();
        live.setId(3); live.setEventId(30); live.setType("producto actualizado"); live.setPayload("{}");
        when(liveMessageRepository.findByEventId(30)).thenReturn(Optional.of(live));
        when(liveMessageRepository.findById(3)).thenReturn(Optional.of(live));

        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("30"); ack.setConsumer(null); ack.setStatus("FAILED");

        service.handleAcknowledgement(ack);

        // Se guarda el consumo con "Desconocido"
        verify(consumptionRepository).save(argThat(mc -> "Desconocido".equals(mc.getModuleName())));
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }
}
