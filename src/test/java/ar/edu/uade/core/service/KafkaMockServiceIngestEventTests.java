package ar.edu.uade.core.service;

import ar.edu.uade.core.model.Event;
import ar.edu.uade.core.model.EventRequest;
import ar.edu.uade.core.model.LiveMessage;
import ar.edu.uade.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceIngestEventTests {

    @Mock private EventRepository eventRepository;
    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private TopicResolver topicResolver;

    @InjectMocks private KafkaMockService service;

    @BeforeEach
    void configureSendSettings() throws Exception {
        setPrivate(service, "sendMaxAttempts", 1);
        setPrivate(service, "sendBackoffMs", 0L);
        setPrivate(service, "sendTimeoutMs", 1000L);
    }

    private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void ingestEvent_WhenValid_SavesAndPublishes() {
        // arrange
        when(topicResolver.resolveTopic("producto creado", "modA")).thenReturn("topicV");
        // eventRepository.save debe devolver el Event con id asignado
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(10);
            return e;
        });
        // kafka send OK
        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq("topicV"), anyString(), any())).thenReturn(CompletableFuture.completedFuture(sendResult));

        EventRequest req = new EventRequest();
        req.setType("producto creado");
        req.setOriginModule("modA");
        req.setTimestamp(LocalDateTime.now());
        req.setPayload(Map.of("x", 1));

        // act
        Event ev = service.ingestEvent(req);

        // assert
        assertNotNull(ev);
        assertEquals(10, ev.getId());
        verify(eventRepository, atLeastOnce()).save(any(Event.class));
        verify(liveMessageRepository).save(any(LiveMessage.class));
        verify(kafkaTemplate).send(eq("topicV"), anyString(), any());
        verifyNoMoreInteractions(retryMessageRepository, deadLetterRepository);
    }

    @Test
    void ingestEvent_WhenKafkaFails_ThrowsIllegalStateException() {
        // arrange
        when(topicResolver.resolveTopic("producto actualizado", "modB")).thenReturn("topicZ");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(20);
            return e;
        });
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        when(kafkaTemplate.send(eq("topicZ"), anyString(), any())).thenReturn(failed);

        EventRequest req = new EventRequest();
        req.setType("producto actualizado");
        req.setOriginModule("modB");
        req.setPayload(Map.of("y", 2));

        // act + assert
        assertThrows(IllegalStateException.class, () -> service.ingestEvent(req));
        verify(eventRepository, atLeastOnce()).save(any(Event.class));
        verify(liveMessageRepository).save(any(LiveMessage.class));
    }
}
