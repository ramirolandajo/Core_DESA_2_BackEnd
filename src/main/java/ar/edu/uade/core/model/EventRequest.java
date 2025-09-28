package ar.edu.uade.core.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class EventRequest {
    private String type;
    private Object payload; // puede ser Map u otro objeto; se serializa a JSON
    private LocalDateTime timestamp; // opcional, si null se setea ahora
    private String originModule;
}

