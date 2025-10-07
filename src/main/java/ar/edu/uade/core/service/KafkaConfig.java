package ar.edu.uade.core.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                 @Value("${app.kafka.startup.request-timeout-ms:5000}") int requestTimeoutMs) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Fail fast / timeouts bajos
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 0);
        // También bajar los backoffs para no alargar los intentos
        configs.put("reconnect.backoff.ms", 250);
        configs.put("reconnect.backoff.max.ms", 1000);
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(true); // cortar el arranque si no hay broker
        return admin;
    }

    @Bean
    public KafkaAdmin.NewTopics topics(TopicResolver resolver){
        return new KafkaAdmin.NewTopics(
            resolver.getAllTopics().stream()
                .map(name -> TopicBuilder.name(name).partitions(3).replicas(1).build())
                .toArray(NewTopic[]::new)
        );
    }
}
