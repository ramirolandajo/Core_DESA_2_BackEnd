package ar.edu.uade.core.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaStartupVerifierDisabledTests {

    private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void verifyConnectivityOnStartup_DisabledByConfig_DoesNotThrow() throws Exception {
        KafkaStartupVerifier v = new KafkaStartupVerifier();
        setPrivate(v, "maxAttempts", 0); // disable
        // bootstrapServers no importa si está deshabilitado
        setPrivate(v, "bootstrapServers", "localhost:9092");

        assertDoesNotThrow(v::verifyConnectivityOnStartup);
    }
}

