package ar.edu.uade.core.service;

import ar.edu.uade.core.model.EventAckEntity;
import ar.edu.uade.core.model.LiveMessage;
import ar.edu.uade.core.model.MessageConsumption;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceAckEdgeTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void handleAcknowledgement_LiveNotFound_ShouldReturn() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("100");
        ack.setConsumer("mod");
        ack.setStatus("SUCCESS");
        when(liveMessageRepository.findByEventId(100)).thenReturn(Optional.empty());

        service.handleAcknowledgement(ack);

        verify(liveMessageRepository).findByEventId(100);
        verifyNoMoreInteractions(liveMessageRepository);
        verifyNoInteractions(retryMessageRepository, deadLetterRepository, consumptionRepository);
    }

    @Test
    void handleAcknowledgement_Fail_ShouldMoveToRetry() {
        EventAckEntity ack = new EventAckEntity();
        ack.setEventId("123");
        ack.setConsumer("mod");
        ack.setStatus("FAIL");

        LiveMessage lm = new LiveMessage();
        lm.setId(1); lm.setEventId(123); lm.setType("T"); lm.setPayload("{}");
        when(liveMessageRepository.findByEventId(123)).thenReturn(Optional.of(lm));
        when(liveMessageRepository.findById(1)).thenReturn(Optional.of(lm));

        service.handleAcknowledgement(ack);

        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(retryMessageRepository).save(any());
        verify(liveMessageRepository).delete(any(LiveMessage.class));
    }
}

