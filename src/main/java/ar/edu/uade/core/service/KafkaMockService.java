package ar.edu.uade.core.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.uade.core.model.BrandDTO;
import ar.edu.uade.core.model.CartDTO;
import ar.edu.uade.core.model.CartItemDTO;
import ar.edu.uade.core.model.CategoryDTO;
import ar.edu.uade.core.model.Event;
import ar.edu.uade.core.model.ProductDTO;
import ar.edu.uade.core.model.PurchaseWithCartDTO;
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


    
    //Instanciando Brands
        
    BrandDTO sony = new BrandDTO(1, "Sony", List.of(101, 102), true);
    BrandDTO apple = new BrandDTO(2, "Apple", List.of(103, 104), true);
    BrandDTO samsung = new BrandDTO(3, "Samsung", List.of(105), true);
    BrandDTO dell = new BrandDTO(4, "Dell", List.of(106, 107), true);
    BrandDTO hp = new BrandDTO(5, "HP", List.of(108), true);
    BrandDTO lenovo = new BrandDTO(6, "Lenovo", List.of(109, 110), true);
    BrandDTO lg = new BrandDTO(7, "LG", List.of(111), true);
    BrandDTO microsoft = new BrandDTO(8, "Microsoft", List.of(112), true);
    BrandDTO asus = new BrandDTO(9, "Asus", List.of(113, 114), true);
    BrandDTO acer = new BrandDTO(10, "Acer", List.of(115), true);    
    
    
    
    // Instanciando  Categories
    CategoryDTO audio = new CategoryDTO(1, "Audio", List.of(101), true);
    CategoryDTO wearables = new CategoryDTO(2, "Wearables", List.of(102), true);
    CategoryDTO smartphones = new CategoryDTO(3, "Smartphones", List.of(103, 104), true);
    CategoryDTO laptops = new CategoryDTO(4, "Laptops", List.of(105, 106), true);
    CategoryDTO tablets = new CategoryDTO(5, "Tablets", List.of(107), true);
    CategoryDTO monitores = new CategoryDTO(6, "Monitores", List.of(108), true);
    CategoryDTO impresoras = new CategoryDTO(7, "Impresoras", List.of(109), true);
    CategoryDTO gaming = new CategoryDTO(8, "Gaming", List.of(110, 111), true);
    CategoryDTO accesorios = new CategoryDTO(9, "Accesorios", List.of(112), true);
    CategoryDTO fotografia = new CategoryDTO(10, "Fotografía", List.of(113), true);

    // Instanciando Products
    ProductDTO p1 = new ProductDTO(101, 1001, "Sony WH-1000XM5", "Auriculares con cancelación de ruido",
        400.0f, 0.05f, 50, List.of(1), 1, 4.9f,
        List.of("https://example.com/sony1.jpg"), true, true, true, false, true);

    ProductDTO p2 = new ProductDTO(102, 1002, "Apple Watch Series 9", "Reloj inteligente con monitor de salud",
            700.0f, 0.10f, 30, List.of(2), 2, 4.8f,
            List.of("https://example.com/watch1.jpg"), true, true, false, true, true);

    ProductDTO p3 = new ProductDTO(103, 1003, "iPhone 15", "Smartphone de última generación",
            1200.0f, 0.0f, 100, List.of(3), 2, 4.7f,
            List.of("https://example.com/iphone.jpg"), true, false, true, true, true);

    ProductDTO p4 = new ProductDTO(104, 1004, "Samsung Galaxy S23", "Smartphone Android gama alta",
            1100.0f, 0.05f, 80, List.of(3), 3, 4.6f,
            List.of("https://example.com/galaxy.jpg"), true, true, false, true, true);

    ProductDTO p5 = new ProductDTO(105, 1005, "Dell XPS 13", "Notebook ultraliviana",
            1500.0f, 0.10f, 40, List.of(4), 4, 4.5f,
            List.of("https://example.com/xps.jpg"), true, false, true, true, true);

    ProductDTO p6 = new ProductDTO(106, 1006, "HP Pavilion", "Notebook gama media",
            900.0f, 0.15f, 60, List.of(4), 5, 4.3f,
            List.of("https://example.com/pavilion.jpg"), true, true, false, false, true);

    ProductDTO p7 = new ProductDTO(107, 1007, "iPad Pro", "Tablet profesional",
            1100.0f, 0.10f, 25, List.of(5), 2, 4.8f,
            List.of("https://example.com/ipad.jpg"), true, true, true, true, true);

    ProductDTO p8 = new ProductDTO(108, 1008, "LG Ultragear", "Monitor gamer 27''",
            450.0f, 0.05f, 70, List.of(6, 8), 7, 4.6f,
            List.of("https://example.com/monitor.jpg"), true, true, false, false, true);

    ProductDTO p9 = new ProductDTO(109, 1009, "Logitech G502", "Mouse gamer",
            80.0f, 0.20f, 200, List.of(8, 9), 9, 4.9f,
            List.of("https://example.com/mouse.jpg"), true, true, true, false, true);

    ProductDTO p10 = new ProductDTO(110, 1010, "Canon EOS R10", "Cámara mirrorless",
            1500.0f, 0.10f, 15, List.of(10), 10, 4.8f,
            List.of("https://example.com/canon.jpg"), true, false, true, true, true);

    //Instanciando CartItem
    CartItemDTO ci1 = new CartItemDTO(1, 2, p1);
    CartItemDTO ci2 = new CartItemDTO(2, 1, p3);
    CartItemDTO ci3 = new CartItemDTO(3, 3, p9);
    
    // Instanciando Cart
    CartDTO c1 = new CartDTO(1, 1200.0f, List.of(ci1, ci2));
    CartDTO c2 = new CartDTO(2, 240.0f, List.of(ci3));
    CartDTO c3 = new CartDTO(3, 2800.0f, List.of(ci1, ci3));

    //Instanciando Purchase
    PurchaseWithCartDTO pur1 = new PurchaseWithCartDTO(1, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
        "Av. Corrientes 1234, CABA", "CONFIRMED", c1);

    PurchaseWithCartDTO pur2 = new PurchaseWithCartDTO(2, LocalDateTime.now(), LocalDateTime.now().plusHours(3),
        "Calle Falsa 742, Springfield", "PENDING", c2);

    PurchaseWithCartDTO pur3 = new PurchaseWithCartDTO(3, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
        "Gran Vía 100, Madrid", "SHIPPED", c3);


    public List<Event>createBrands(List<BrandDTO>brandsToCreate){
        List<Event>eventsToSend = new ArrayList<>();

        for (BrandDTO b : brandsToCreate) {
            eventsToSend.add(sendEvent("POST: Marca creada", b));
        }

        return eventRepository.saveAll(eventsToSend);
    }

    public List<Event>createCateogies(List<CategoryDTO>categoriesToCreate){
        List<Event> eventsToSend = new ArrayList<>();

        for (CategoryDTO c : categoriesToCreate) {
            eventsToSend.add(sendEvent("POST: Categorias creadas", c));            
        }
        
        return eventRepository.saveAll(eventsToSend);
    }

    public List<Event>createProducts(List<ProductDTO>productsToSave){
        List<Event>eventsToSend = new ArrayList<>();

        for (ProductDTO p : productsToSave) {
            eventsToSend.add(sendEvent("POST: productos creados", p));
        }
        
        return eventRepository.saveAll(eventsToSend);
    }

    public List<Event>createCarts(List<CartDTO>cartsToSave){
        List<Event> eventsToSend = new ArrayList<>();

        for (CartDTO c : cartsToSave) {
            eventsToSend.add(sendEvent("POST: carrito actualizado", c));
        }

        return eventRepository.saveAll(eventsToSend);
    }

    public List<Event>createPurchase(List<PurchaseWithCartDTO>purchaseToSave){
        List<Event> eventsToSend = new ArrayList<>();

        for (PurchaseWithCartDTO p : purchaseToSave) {
            eventsToSend.add(sendEvent("POST: Compra confirmada", p));
        }

        return eventRepository.saveAll(eventsToSend);
    }

    
}

