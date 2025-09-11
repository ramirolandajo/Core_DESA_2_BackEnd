package ar.edu.uade.core.controller;

import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.Media;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.core.model.Event;
import ar.edu.uade.core.service.KafkaMockService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



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
    
    @PostMapping(value = "/transmit")
    public ResponseEntity<?>transmit(){
        // Como esto devuelve un listado completo, para que no quede
        // una "catarata de eventos" todo de una, desde el front cuando se
        // itere el listado se le va a poner un sleep que simule que los 
        // eventos ocurren ent iempo real. SOLO PARA LA PRIMERA ENTREGA
        List<Event> events = new ArrayList<>();

        try {
            events.addAll(kafkaMockService.createBrand());
            
            events.addAll(kafkaMockService.createCategory());
            
            events.addAll(kafkaMockService.createProduct());
            
            events.addAll(kafkaMockService.updateProductPrice());
        
            events.addAll(kafkaMockService.updateProductStockIncrease());

            events.addAll(kafkaMockService.updateProductGeneral());

            events.addAll(kafkaMockService.createCart());

            events.addAll(kafkaMockService.updateCartAddProduct());

            events.addAll(kafkaMockService.updateCartRemoveProduct());

            events.addAll(kafkaMockService.createPurchase());

            events.addAll(kafkaMockService.deactivateProduct());
            
            events.addAll(kafkaMockService.deactivateProduct());
            
            return new ResponseEntity<>(events,HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
        
}
    

