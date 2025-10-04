package ar.edu.uade.core.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class KafkaStartupVerifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaStartupVerifier.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.startup.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.kafka.startup.backoff-ms:1000}")
    private long backoffMs;

    @Value("${app.kafka.startup.request-timeout-ms:5000}")
    private long requestTimeoutMs;

    @PostConstruct
    public void verifyConnectivityOnStartup() {
        // Si maxAttempts <= 0, desactivar verificación para entornos que no la requieran
        if (maxAttempts <= 0) {
            log.info("[KafkaStartup] Verificación de arranque desactivada (maxAttempts <= 0)");
            return;
        }
        int attempt = 0;
        Exception lastEx = null;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                log.info("[KafkaStartup] Verificando conexión a Kafka ({}), intento {}/{}", bootstrapServers, attempt, maxAttempts);
                Map<String, Object> cfg = new HashMap<>();
                cfg.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                cfg.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) requestTimeoutMs);
                cfg.put(AdminClientConfig.RETRIES_CONFIG, 0); // que este check no reintente internamente
                try (AdminClient admin = AdminClient.create(cfg)) {
                    admin.describeCluster()
                         .nodes()
                         .get(requestTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    log.info("[KafkaStartup] Conexión a Kafka OK: {}", bootstrapServers);
                    return; // éxito
                }
            } catch (Exception ex) {
                lastEx = ex;
                log.warn("[KafkaStartup] Falló la conexión a Kafka ({}) en el intento {}/{}. {}: {}", bootstrapServers, attempt, maxAttempts, ex.getClass().getSimpleName(), ex.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        String msg = String.format("No se pudo conectar a Kafka en %d intentos. bootstrapServers=%s", maxAttempts, bootstrapServers);
        log.error("[KafkaStartup] {}", msg, lastEx);
        throw new IllegalStateException(msg, lastEx);
    }
}
