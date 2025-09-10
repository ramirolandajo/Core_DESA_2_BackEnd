package ar.edu.uade.core.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.uade.core.model.Event;
import ar.edu.uade.core.model.ProductDTO;
import ar.edu.uade.core.repository.EventRepository;

@Service
public class KafkaMockService {

    // NOS INTERESA
    /*
     * Productos: creacion, modificacion parcial(precio,stock), modificacion total, borrado(desactivacion)
     * Marca: creacion, producto agregado, borrado(desactivacion)
     * categoria: creacion, producto agregado, borrado(desactivacion)
     * 
     * Compra: confirmacion de compra, cancelacion de compra 
     */
    

    @Autowired
    EventRepository eventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Event sendEvent(String type, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            Event event = new Event(type, payloadJson);
            
            eventRepository.save(event);
            return event;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando payload a JSON", e);
        }
    }

}

