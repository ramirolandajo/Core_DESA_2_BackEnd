package ar.edu.uade.core.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.uade.core.mock.MockDataFactory;
import ar.edu.uade.core.model.Event;
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

    private final MockDataFactory mock = new MockDataFactory();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*public Event sendEvent(String type, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            Event event = new Event(type, payloadJson);
            
            eventRepository.save(event);
            return event;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando payload a JSON", e);
        }
    }*/

    private Event sendEvent(String type, Object payload) {
    try {
        // si falla, lo guarda con toString()
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            payloadJson = payload.toString();
        }
        return new Event(type, payloadJson);
    } catch (Exception e) {
        throw new RuntimeException("Error serializando payload", e);
    }
}


    public List<Event>getAll(){
        return eventRepository.findAll();
    }

    // BRANDS
    public List<Event> createBrand() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("POST: Marca creada", mock.sony()));
        eventsToSave.add(sendEvent("POST: Marca creada", mock.apple()));
        return eventRepository.saveAll(eventsToSave);
    }

    // CATEGORIES
    public List<Event> createCategory() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("POST: Categoría creada", mock.audio()));
        eventsToSave.add(sendEvent("POST: Categoría creada", mock.wearables()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> deactivateCategory() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PATCH: Categoría desactivada", mock.audioInactive()));
        return eventRepository.saveAll(eventsToSave);
    }

    // PRODUCTS
    public List<Event> createProduct() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("POST: Producto creado", mock.sonyHeadphones()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateProductStockDecrease() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PATCH: Stock disminuido", mock.sonyHeadphonesDecreaseStock()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateProductStockIncrease() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PATCH: Stock aumentado", mock.sonyHeadphonesIncreaseStock()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateProductPrice() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PATCH: Precio actualizado", mock.sonyHeadphonesPriceChange()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateProductGeneral() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PUT: Producto actualizado", mock.sonyHeadphonesUpdated()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> deactivateProduct() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PATCH: Producto desactivado", mock.sonyHeadphonesDeactivate()));
        return eventRepository.saveAll(eventsToSave);
    }

    // CARTS
    public List<Event> createCart() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("POST: Carrito creado", mock.cartWithSony()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateCartAddProduct() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PUT: Producto agregado al carrito", mock.cartAddAppleWatch()));
        return eventRepository.saveAll(eventsToSave);
    }

    public List<Event> updateCartRemoveProduct() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("PUT: Producto eliminado del carrito", mock.cartRemoveSony()));
        return eventRepository.saveAll(eventsToSave);
    }

    // PURCHASES
    public List<Event> createPurchase() {
        List<Event> eventsToSave = new ArrayList<>();
        eventsToSave.add(sendEvent("POST: Compra pendiente", mock.purchasePending()));
        eventsToSave.add(sendEvent("PATCH: Compra confirmada", mock.purchaseConfirmed()));
        eventsToSave.add(sendEvent("PATCH: Compra enviada", mock.purchaseShipped()));
        return eventRepository.saveAll(eventsToSave);
    }
}

