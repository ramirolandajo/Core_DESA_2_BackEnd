package ar.edu.uade.core.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AcknowledgementRequest {
    private Integer eventId;
    private Integer liveMessageId;
    private String moduleName;
    private String status;          // SUCCESS | FAIL
    private String errorMessage;    // si el status es "FAIL" tiene contenido sino vacio
    private LocalDateTime timestamp;
}

