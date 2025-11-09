package ar.edu.uade.core.controller;

import ar.edu.uade.core.model.*;
import ar.edu.uade.core.service.KafkaMockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CoreController.class)
public class CoreControllerAdditionalTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaMockService service;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public KafkaMockService kafkaMockService() { return Mockito.mock(KafkaMockService.class); }
    }

    private final ObjectMapper mapper = new ObjectMapper();



}
