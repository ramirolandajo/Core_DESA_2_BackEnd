package ar.edu.uade.core.controller;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.service.KafkaMockService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests del controlador /core usando MockMvc.
 */
@WebMvcTest(CoreController.class)
class CoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaMockService service;

    @Test
    void getAll_ShouldReturnEvents() throws Exception {
        Event e = new Event();
        e.setId(11);
        e.setType("POST: x");
        e.setPayload("{}");
        e.setTimestamp(LocalDateTime.now());
        e.setOriginModule("Ventas");

        when(service.getAll()).thenReturn(List.of(e));

        mockMvc.perform(get("/core/getAll").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].type").value("POST: x"));
    }

    @Test
    void transmit_ShouldReturnAggregatedEvents() throws Exception {
        when(service.createBrand()).thenReturn(List.of(new Event("t1","{}","Ventas")));
        when(service.createCategory()).thenReturn(List.of(new Event("t2","{}","Ventas")));
        when(service.createProduct()).thenReturn(List.of(new Event("t3","{}","Ventas")));
        when(service.updateProductPrice()).thenReturn(List.of(new Event("t4","{}","Ventas")));
        when(service.updateProductStockIncrease()).thenReturn(List.of(new Event("t5","{}","Ventas")));
        when(service.updateProductGeneral()).thenReturn(List.of(new Event("t6","{}","Ventas")));
        when(service.createCart()).thenReturn(List.of(new Event("t7","{}","Ventas")));
        when(service.updateCartAddProduct()).thenReturn(List.of(new Event("t8","{}","Ventas")));
        when(service.updateCartRemoveProduct()).thenReturn(List.of(new Event("t9","{}","Ventas")));
        when(service.createPurchase()).thenReturn(List.of(new Event("t10","{}","Ventas")));
        when(service.deactivateProduct()).thenReturn(List.of(new Event("t11","{}","Ventas")));

        mockMvc.perform(post("/core/transmit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void liveRetriesDead_ShouldReturnLists() throws Exception {
        when(service.getLiveMessages()).thenReturn(List.of(new LiveMessage()));
        when(service.getRetryMessages()).thenReturn(List.of(new RetryMessage()));
        when(service.getDeadLetters()).thenReturn(List.of(new DeadLetterMessage()));

        mockMvc.perform(get("/core/live")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/core/retries")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/core/dead")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void consumeAll_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/core/consumeAll"))
                .andExpect(status().isOk());
        verify(service).consumeAllLive();
    }

    @Test
    void consumeOne_ShouldReturnRetry() throws Exception {
        RetryMessage rm = new RetryMessage();
        rm.setId(99);
        rm.setEventId(77);

        ConsumeResult ret = new ConsumeResult();
        ret.setStatus(ConsumeResult.Status.RETRY);
        ret.setRetryMessage(rm);

        when(service.consumeOneAndMoveToRetry(isNull(), eq(77), eq("Analytics"))).thenReturn(ret);

        mockMvc.perform(post("/core/consumeOne")
                        .param("eventId", "77")
                        .param("module", "Analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.eventId").value(77));
    }

    @Test
    void consumeOne_ShouldReturnConflict() throws Exception {
        ConsumeResult ret = new ConsumeResult();
        ret.setStatus(ConsumeResult.Status.CONFLICT);
        ret.setMessage("Already consumed");

        when(service.consumeOneAndMoveToRetry(isNull(), eq(77), eq("Ventas"))).thenReturn(ret);

        mockMvc.perform(post("/core/consumeOne")
                        .param("eventId", "77")
                        .param("module", "Ventas"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Already consumed"));
    }

    @Test
    void retryFail_ShouldReturnDead() throws Exception {
        DeadLetterMessage dm = new DeadLetterMessage();
        dm.setId(7);
        dm.setEventId(77);

        ConsumeResult ret = new ConsumeResult();
        ret.setStatus(ConsumeResult.Status.DEAD);
        ret.setDeadLetterMessage(dm);

        when(service.failRetry(123)).thenReturn(ret);

        mockMvc.perform(post("/core/retry/fail")
                        .param("retryId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.eventId").value(77));
    }

    @Test
    void processRetries_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/core/processRetries"))
                .andExpect(status().isOk());
        verify(service).processRetriesAndExpire();
    }
}
