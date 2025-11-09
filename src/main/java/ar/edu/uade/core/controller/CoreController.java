package ar.edu.uade.core.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.edu.uade.core.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.core.service.KafkaMockService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping(value = "/core")
public class CoreController {

    @Autowired
    KafkaMockService kafkaMockService;

    @GetMapping(value = "/getAll", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Event>> getAll(){
        try{ 
            List<Event> events = kafkaMockService.getAll();
            return new ResponseEntity<>(events,HttpStatus.OK);
        }catch(EmptyResultDataAccessException e){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    // Nuevo endpoint: recibe eventos del middleware y los publica a Kafka
    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveEvent(@RequestBody EventRequest request){
        try {
            Event saved = kafkaMockService.ingestEvent(request);
            return new ResponseEntity<>(saved, HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException iae){
            return new ResponseEntity<>(iae.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoints para manejar listas de mensajes (vivos, reintentos, muertos)
    @GetMapping(value = "/live", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<LiveMessage>> getLiveMessages(){
        List<LiveMessage> items = kafkaMockService.getLiveMessages();
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @GetMapping(value = "/retries", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RetryMessage>> getRetryMessages(){
        List<RetryMessage> items = kafkaMockService.getRetryMessages();
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @GetMapping(value = "/dead", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<DeadLetterMessage>> getDeadLetters(){
        List<DeadLetterMessage> items = kafkaMockService.getDeadLetters();
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    // Simular que todos los consumidores procesaron los mensajes vivos (utilidad)
    @PostMapping(value = "/consumeAll")
    public ResponseEntity<?> consumeAll(){
        kafkaMockService.consumeAllLive();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Simular que un consumidor tomó un mensaje y espera respuesta -> mover a retry
    // Acepta opcionalmente liveMessageId o eventId. Se recomienda enviar eventId
    @PostMapping(value = "/consumeOne")
    public ResponseEntity<?> consumeOne(
            @RequestParam(required = false) Integer liveMessageId,
            @RequestParam(required = false) Integer eventId,
            @RequestParam String module){
        ConsumeResult result = kafkaMockService.consumeOneAndMoveToRetry(liveMessageId, eventId, module);
        switch(result.getStatus()){
            case NOT_FOUND:
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            case CONFLICT:
                return new ResponseEntity<>(result.getMessage(), HttpStatus.CONFLICT);
            case RETRY:
                return new ResponseEntity<>(result.getRetryMessage(), HttpStatus.OK);
            case DEAD:
                return new ResponseEntity<>(result.getDeadLetterMessage(), HttpStatus.OK);
            default:
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Consumir y devolver auditoría de consumos del evento (uso práctico de getConsumptions)
    @PostMapping(value = "/consumeOneWithAudit")
    public ResponseEntity<?> consumeOneWithAudit(
            @RequestParam(required = false) Integer liveMessageId,
            @RequestParam(required = false) Integer eventId,
            @RequestParam String module){
        ConsumeResult result = kafkaMockService.consumeOneAndMoveToRetry(liveMessageId, eventId, module);
        if(result.getStatus() == ConsumeResult.Status.NOT_FOUND) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(result.getStatus() == ConsumeResult.Status.CONFLICT) return new ResponseEntity<>(result.getMessage(), HttpStatus.CONFLICT);

        Integer evtId = null;
        Object resBody = null;
        if(result.getStatus() == ConsumeResult.Status.RETRY){
            evtId = result.getRetryMessage() != null ? result.getRetryMessage().getEventId() : null;
            resBody = result.getRetryMessage();
        } else if(result.getStatus() == ConsumeResult.Status.DEAD){
            evtId = result.getDeadLetterMessage() != null ? result.getDeadLetterMessage().getEventId() : null;
            resBody = result.getDeadLetterMessage();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("result", resBody);
        if(evtId != null){
            payload.put("consumptions", kafkaMockService.getConsumptions(null, evtId));
        } else {
            payload.put("consumptions", java.util.Collections.emptyList());
        }
        return new ResponseEntity<>(payload, HttpStatus.OK);
    }

    // Mark retry as failed (increment attempts / move to dead si corresponde)
    @PostMapping(value = "/retry/fail")
    public ResponseEntity<?> failRetry(@RequestParam Integer retryId){
        ConsumeResult result = kafkaMockService.failRetry(retryId);
        switch(result.getStatus()){
            case NOT_FOUND:
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            case RETRY:
                return new ResponseEntity<>(result.getRetryMessage(), HttpStatus.OK);
            case DEAD:
                return new ResponseEntity<>(result.getDeadLetterMessage(), HttpStatus.OK);
            default:
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Procesar reintentos y expirados -> mover a dead-letter
    @PostMapping(value = "/processRetries")
    public ResponseEntity<?> processRetries(){
        kafkaMockService.processRetriesAndExpire();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/ack")
    public ResponseEntity<String> acknowledgeEvent(@RequestBody EventAckEntity ack) {
        try {
            kafkaMockService.handleAcknowledgement(ack);
            return ResponseEntity.ok("ACK recibido correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando ACK: " + e.getMessage());
        }
    }


}
