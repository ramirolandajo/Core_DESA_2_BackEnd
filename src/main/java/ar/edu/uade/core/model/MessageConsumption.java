package ar.edu.uade.core.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "message_consumption")
public class MessageConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "live_message_id")
    private Integer liveMessageId;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

}

