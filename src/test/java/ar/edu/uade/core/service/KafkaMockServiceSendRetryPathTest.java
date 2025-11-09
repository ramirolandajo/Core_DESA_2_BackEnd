package ar.edu.uade.core.service;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceSendRetryPathTest {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @BeforeEach
    void tune() {
        ReflectionTestUtils.setField(service, "sendMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "sendBackoffMs", 1L);
        ReflectionTestUtils.setField(service, "sendTimeoutMs", 50L);
    }

    @Test
    void ingestEvent_WhenFirstSendReturnsNullAndSecondSucceeds_ShouldRetryAndSucceed() {
        EventRequest req = new EventRequest();
        req.setType("ventas.creada");
        req.setOriginModule("ventas");
        req.setPayload("x");

        Event ev = new Event(); ev.setId(99); ev.setType(req.getType()); ev.setPayload("x"); ev.setOriginModule("ventas");
        when(eventRepository.save(any(Event.class))).thenReturn(ev);
        when(topicResolver.resolveTopic(anyString(), anyString())).thenReturn("t");

        // primer future completado con valor null
        CompletableFuture<SendResult<String, Object>> first = CompletableFuture.completedFuture(null);
        // segundo future con SendResult mockeado tipado
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sr = (SendResult<String, Object>) mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> second = CompletableFuture.completedFuture(sr);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(first).thenReturn(second);

        Event result = service.ingestEvent(req);
        assertEquals(99, result.getId());
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        verify(liveMessageRepository).save(any(LiveMessage.class));
    }
}
