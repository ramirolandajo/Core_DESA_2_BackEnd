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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceConsumeFromLiveTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @Test
    void consumeOne_LiveFound_SameOrigin_ReturnsConflict() {
        LiveMessage lm = new LiveMessage();
        lm.setId(1); lm.setEventId(100); lm.setType("T"); lm.setPayload("P"); lm.setOriginModule("modA");
        when(liveMessageRepository.findById(1)).thenReturn(java.util.Optional.of(lm));

        ConsumeResult r = service.consumeOneAndMoveToRetry(1, null, "modA");
        assertEquals(ConsumeResult.Status.CONFLICT, r.getStatus());
        verifyNoInteractions(retryMessageRepository, deadLetterRepository);
    }

    @Test
    void consumeOne_LiveFound_NewConsumer_CreatesRetryAndDeletesLive() {
        LiveMessage lm = new LiveMessage();
        lm.setId(2); lm.setEventId(101); lm.setType("T"); lm.setPayload("P"); lm.setOriginModule("ventas");
        when(liveMessageRepository.findById(2)).thenReturn(java.util.Optional.of(lm));
        when(consumptionRepository.findByEventId(101)).thenReturn(List.of());

        ConsumeResult r = service.consumeOneAndMoveToRetry(2, null, "inventario");

        assertEquals(ConsumeResult.Status.RETRY, r.getStatus());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(liveMessageRepository).deleteById(2);
    }
}

