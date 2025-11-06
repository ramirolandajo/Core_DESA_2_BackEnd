package ar.edu.uade.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class KafkaStartupVerifierMoreTests {

    @Test
    void verifyConnectivityOnStartup_DisabledWhenMaxAttemptsNegative() {
        KafkaStartupVerifier verifier = new KafkaStartupVerifier();
        ReflectionTestUtils.setField(verifier, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(verifier, "maxAttempts", -1);
        assertDoesNotThrow(verifier::verifyConnectivityOnStartup);
    }
}

