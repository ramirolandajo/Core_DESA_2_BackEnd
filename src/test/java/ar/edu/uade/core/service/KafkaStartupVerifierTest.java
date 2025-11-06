package ar.edu.uade.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class KafkaStartupVerifierTest {

    @Test
    void verifyConnectivityOnStartup_DisabledWhenMaxAttemptsZero() {
        KafkaStartupVerifier verifier = new KafkaStartupVerifier();
        ReflectionTestUtils.setField(verifier, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(verifier, "maxAttempts", 0);
        // No debe lanzar excepción ni intentar conexiones
        assertDoesNotThrow(verifier::verifyConnectivityOnStartup);
    }
}

