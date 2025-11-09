package ar.edu.uade.core.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventMessage {
    private String eventId;         // UUID
    private String eventType;       // e.g., ventas.compra-confirmada
    private OffsetDateTime timestamp; // ISO-8601 con Z
    private String originModule;    // p.ej. "ventas", "middleware"
    private Object payload;         // contenido específico
}

