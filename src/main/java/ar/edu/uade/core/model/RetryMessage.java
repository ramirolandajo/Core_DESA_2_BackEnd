package ar.edu.uade.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "retry_message")
public class RetryMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_id")
    private Integer eventId;

    // si vino de un live_message, guardamos el id original para buscar por live id
    @Column(name = "original_live_id")
    private Integer originalLiveId;

    private String type;

    @Lob
    private String payload;

    private Integer attempts;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "ttl_seconds")
    private Long ttlSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    // módulo que está procesando este retry
    @Column(name = "consumer_module")
    private String consumerModule;

}
