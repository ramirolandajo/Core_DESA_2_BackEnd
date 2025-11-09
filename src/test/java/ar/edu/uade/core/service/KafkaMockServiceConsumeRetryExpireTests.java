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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMockServiceConsumeRetryExpireTests {

    @Mock private LiveMessageRepository liveMessageRepository;
    @Mock private RetryMessageRepository retryMessageRepository;
    @Mock private DeadLetterRepository deadLetterRepository;
    @Mock private MessageConsumptionRepository consumptionRepository;
    @Mock private EventRepository eventRepository;

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
    void consumeOne_FromLive_FirstConsumer_CreatesRetryAndDeletesLive() {
        LiveMessage live = new LiveMessage();
        live.setId(1); live.setEventId(10); live.setType("estado pendiente"); live.setPayload("{}"); live.setOriginModule("core");
        when(liveMessageRepository.findByEventId(10)).thenReturn(Optional.of(live));
        when(consumptionRepository.existsByEventIdAndModuleName(10, "inventario")).thenReturn(false);

        ConsumeResult res = service.consumeOneAndMoveToRetry(null, 10, "inventario");

        assertEquals(ConsumeResult.Status.RETRY, res.getStatus());
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(liveMessageRepository).deleteById(1);
        verify(consumptionRepository).save(any(MessageConsumption.class));
    }

    @Test
    void consumeOne_FromRetry_SameOrigin_Conflict() {
        RetryMessage rm = new RetryMessage();
        rm.setId(3); rm.setEventId(30); rm.setType("producto"); rm.setPayload("{}"); rm.setAttempts(1);
        when(retryMessageRepository.findByEventId(30)).thenReturn(Optional.of(rm));
        Event ev = new Event(); ev.setId(30); ev.setOriginModule("modA");
        when(eventRepository.findById(30)).thenReturn(Optional.of(ev));

        ConsumeResult res = service.consumeOneAndMoveToRetry(null, 30, "modA");
        assertEquals(ConsumeResult.Status.CONFLICT, res.getStatus());
        verify(consumptionRepository, never()).save(any());
    }

    @Test
    void consumeOne_FromRetry_NewConsumer_UnderThreshold_StoresConsumptionAndIncrementsAttempts() {
        RetryMessage rm = new RetryMessage();
        rm.setId(4); rm.setEventId(40); rm.setType("producto"); rm.setPayload("{}"); rm.setAttempts(0); rm.setMaxAttempts(5); rm.setCreatedAt(LocalDateTime.now()); rm.setTtlSeconds(3600L);
        when(retryMessageRepository.findByEventId(40)).thenReturn(Optional.of(rm));
        Event ev = new Event(); ev.setId(40); ev.setOriginModule("modA");
        when(eventRepository.findById(40)).thenReturn(Optional.of(ev));
        when(consumptionRepository.existsByEventIdAndModuleName(40, "modB")).thenReturn(false);
        when(consumptionRepository.findByEventId(40)).thenReturn(new ArrayList<>());

        ConsumeResult res = service.consumeOneAndMoveToRetry(null, 40, "modB");
        assertEquals(ConsumeResult.Status.RETRY, res.getStatus());
        verify(consumptionRepository).save(any(MessageConsumption.class));
        verify(retryMessageRepository).save(any(RetryMessage.class));
        verify(deadLetterRepository, never()).save(any());
    }

    @Test
    void acknowledgeRetry_RemovesAndReturnsTrue() {
        when(retryMessageRepository.existsById(5)).thenReturn(true);
        boolean ok = service.acknowledgeRetry(5);
        assertTrue(ok);
        verify(retryMessageRepository).deleteById(5);
    }

    @Test
    void failRetry_MaxAttempts_DeadLetter() {
        RetryMessage rm = new RetryMessage();
        rm.setId(6); rm.setEventId(60); rm.setType("t"); rm.setPayload("{}"); rm.setAttempts(2); rm.setMaxAttempts(3);
        when(retryMessageRepository.findById(6)).thenReturn(Optional.of(rm));

        ConsumeResult res = service.failRetry(6);
        assertEquals(ConsumeResult.Status.DEAD, res.getStatus());
        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(6);
    }

    @Test
    void processRetriesAndExpire_TtlExpired_MovesToDead() {
        RetryMessage r = new RetryMessage();
        r.setId(7); r.setEventId(70); r.setType("x"); r.setPayload("{}"); r.setAttempts(0); r.setMaxAttempts(3);
        r.setTtlSeconds(1L); r.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(retryMessageRepository.findAll()).thenReturn(List.of(r));

        service.processRetriesAndExpire();

        verify(deadLetterRepository).save(any(DeadLetterMessage.class));
        verify(retryMessageRepository).deleteById(7);
    }
}

