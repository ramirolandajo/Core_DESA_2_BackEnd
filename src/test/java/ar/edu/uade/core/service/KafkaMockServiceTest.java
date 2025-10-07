//package ar.edu.uade.core.service;
//
//import ar.edu.uade.core.model.*;
//import ar.edu.uade.core.repository.*;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class KafkaMockServiceTest {
//
//    @Mock
//    private EventRepository eventRepository;
//    @Mock
//    private LiveMessageRepository liveMessageRepository;
//    @Mock
//    private RetryMessageRepository retryMessageRepository;
//    @Mock
//    private DeadLetterRepository deadLetterRepository;
//    @Mock
//    private MessageConsumptionRepository consumptionRepository;
//
//    @InjectMocks
//    private KafkaMockService kafkaMockService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void sendEvent_ShouldPersistEventAndLiveMessage() {
//        Event e = new Event("POST: Producto creado", "{}", "MOCK");
//        e.setId(1);
//
//        when(eventRepository.save(any(Event.class))).thenReturn(e);
//
//        Event result = kafkaMockService.createProduct().get(0);
//
//        assertNotNull(result);
//        assertEquals("POST: Producto creado", result.getType());
//        verify(liveMessageRepository, atLeastOnce()).save(any(LiveMessage.class));
//    }
//
//    @Test
//    void consumeOneAndMoveToRetry_ShouldMoveLiveToRetry() {
//        Event ev = new Event("POST: test", "{}", "Ventas");
//        ev.setId(1);
//
//        LiveMessage lm = new LiveMessage();
//        lm.setId(10);
//        lm.setEventId(1);
//        lm.setOriginModule("Inventario");
//        lm.setType(ev.getType());
//        lm.setPayload(ev.getPayload());
//        lm.setTimestamp(LocalDateTime.now());
//
//        when(liveMessageRepository.findById(10)).thenReturn(Optional.of(lm));
//        when(eventRepository.findById(1)).thenReturn(Optional.of(ev));
//        when(consumptionRepository.existsByEventIdAndModuleName(1, "Analytics")).thenReturn(false);
//
//        ConsumeResult result = kafkaMockService.consumeOneAndMoveToRetry(10, null, "Analytics");
//
//        assertEquals(ConsumeResult.Status.RETRY, result.getStatus());
//        verify(retryMessageRepository, times(1)).save(any(RetryMessage.class));
//        verify(liveMessageRepository, times(1)).deleteById(10);
//    }
//
//    @Test
//    void consumeOneAndMoveToRetry_ShouldReturnConflict_WhenSameOriginModule() {
//        LiveMessage lm = new LiveMessage();
//        lm.setId(10);
//        lm.setEventId(1);
//        lm.setOriginModule("Ventas");
//
//        when(liveMessageRepository.findById(10)).thenReturn(Optional.of(lm));
//
//        ConsumeResult result = kafkaMockService.consumeOneAndMoveToRetry(10, null, "Ventas");
//
//        assertEquals(ConsumeResult.Status.CONFLICT, result.getStatus());
//    }
//
//    @Test
//    void acknowledgeRetry_ShouldDeleteRetryMessage() {
//        when(retryMessageRepository.existsById(5)).thenReturn(true);
//
//        boolean result = kafkaMockService.acknowledgeRetry(5);
//
//        assertTrue(result);
//        verify(retryMessageRepository, times(1)).deleteById(5);
//    }
//
//    @Test
//    void failRetry_ShouldMoveToDeadLetter_WhenMaxAttemptsExceeded() {
//        RetryMessage rm = new RetryMessage();
//        rm.setId(1);
//        rm.setEventId(100);
//        rm.setAttempts(3);
//        rm.setMaxAttempts(3);
//        rm.setType("PATCH");
//        rm.setPayload("{}");
//
//        when(retryMessageRepository.findById(1)).thenReturn(Optional.of(rm));
//
//        ConsumeResult result = kafkaMockService.failRetry(1);
//
//        assertEquals(ConsumeResult.Status.DEAD, result.getStatus());
//        verify(deadLetterRepository, times(1)).save(any(DeadLetterMessage.class));
//        verify(retryMessageRepository, times(1)).deleteById(1);
//    }
//
//    @Test
//    void failRetry_ShouldIncrementAttempts_WhenBelowMax() {
//        RetryMessage rm = new RetryMessage();
//        rm.setId(1);
//        rm.setEventId(100);
//        rm.setAttempts(1);
//        rm.setMaxAttempts(3);
//
//        when(retryMessageRepository.findById(1)).thenReturn(Optional.of(rm));
//
//        ConsumeResult result = kafkaMockService.failRetry(1);
//
//        assertEquals(ConsumeResult.Status.RETRY, result.getStatus());
//        verify(retryMessageRepository, times(1)).save(any(RetryMessage.class));
//    }
//
//    @Test
//    void processRetriesAndExpire_ShouldMoveExpiredRetryToDeadLetter() {
//        RetryMessage rm = new RetryMessage();
//        rm.setId(1);
//        rm.setEventId(200);
//        rm.setAttempts(1);
//        rm.setMaxAttempts(5);
//        rm.setCreatedAt(LocalDateTime.now().minusHours(2));
//        rm.setTtlSeconds(3600L);
//
//        when(retryMessageRepository.findAll()).thenReturn(List.of(rm));
//
//        kafkaMockService.processRetriesAndExpire();
//
//        verify(deadLetterRepository, times(1)).save(any(DeadLetterMessage.class));
//        verify(retryMessageRepository, times(1)).deleteById(1);
//    }
//
//    @Test
//    void messageLookup_ShouldReturnLiveLocation() {
//        LiveMessage lm = new LiveMessage();
//        lm.setId(1);
//        lm.setEventId(100);
//        lm.setOriginModule("Ventas");
//
//        when(liveMessageRepository.findById(1)).thenReturn(Optional.of(lm));
//
//        MessageLookupResult res = kafkaMockService.messageLookup(1, null);
//
//        assertEquals("LIVE", res.getLocation());
//        assertEquals(100, res.getEventId());
//    }
//
//    @Test
//    void messageLookup_ShouldReturnRetryLocation() {
//        RetryMessage rm = new RetryMessage();
//        rm.setId(2);
//        rm.setEventId(200);
//        rm.setType("PATCH");
//        rm.setPayload("{}");
//
//        when(retryMessageRepository.findById(2)).thenReturn(Optional.of(rm));
//
//        MessageLookupResult res = kafkaMockService.messageLookup(2, null);
//
//        assertEquals("RETRY", res.getLocation());
//        assertEquals(200, res.getEventId());
//    }
//
//    @Test
//    void messageLookup_ShouldReturnDeadLocation() {
//        DeadLetterMessage dm = new DeadLetterMessage();
//        dm.setEventId(300);
//        dm.setType("POST");
//        dm.setPayload("{}");
//
//        when(deadLetterRepository.findAll()).thenReturn(List.of(dm));
//
//        MessageLookupResult res = kafkaMockService.messageLookup(null, 300);
//
//        assertEquals("DEAD", res.getLocation());
//        assertEquals(300, res.getEventId());
//    }
//}
