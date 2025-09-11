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
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "type")
    private String type;

    @Lob
    private String payload;

    private LocalDateTime timestamp;

    // origen del modulo que generó el evento (Ventas, Inventaria, Analitica, MOCK...)
    @Column(name = "origin_module")
    private String originModule;

    public Event(String type, String payload){
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public Event(String type, String payload, String originModule){
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
        this.originModule = originModule;
    }

    @Override
     public String toString() { 
        return "Event{" + 
            "type='" + type +
            '\'' + ", payload=" + payload + 
            ", timestamp=" + timestamp +
            ", originModule=" + originModule +
          '}';
    }

}
