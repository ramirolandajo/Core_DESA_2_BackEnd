package ar.edu.uade.core.mock;

import ar.edu.uade.core.model.ProductDTO;
import ar.edu.uade.core.model.*;

import java.time.LocalDateTime;
import java.util.List;

public class MockDataFactory {

    // BRANDS    
    public static BrandDTO sony() {
        return new BrandDTO(1, "Sony", List.of(101, 102), true);
    }

    public static BrandDTO apple() {
        return new BrandDTO(2, "Apple", List.of(103, 104), true);
    }

    
    // CATEGORIES
    public static CategoryDTO audio() {
        return new CategoryDTO(1, "Audio", List.of(101), true);
    }

    public static CategoryDTO wearables() {
        return new CategoryDTO(2, "Wearables", List.of(102, 103), true);
    }

    // Desactivar categoría
    public static CategoryDTO audioInactive() {
        CategoryDTO cat = audio();
        cat.setActive(false);
        return cat;
    }

    // PRODUCTS
    public static ProductDTO sonyHeadphones() {
        return new ProductDTO(
                101, 1001, "Sony WH-1000XM5", "Auriculares con cancelación de ruido",
                400.0f, 0.05f, 50,
                List.of(1), 1, 4.9f,
                List.of("https://example.com/sony1.jpg"),
                true, true, true, false, true
        );
    }

    // Disminuir stock
    public static ProductDTO sonyHeadphonesDecreaseStock() {
        ProductDTO p = sonyHeadphones();
        p.setStock(25);
        return p;
    }

    // Aumentar stock
    public static ProductDTO sonyHeadphonesIncreaseStock() {
        ProductDTO p = sonyHeadphones();
        p.setStock(100);
        return p;
    }

    // Cambiar precio
    public static ProductDTO sonyHeadphonesPriceChange() {
        ProductDTO p = sonyHeadphones();
        p.setUnitPrice(350.0f);
        return p;
    }

    // Actualización general
    public static ProductDTO sonyHeadphonesUpdated() {
        ProductDTO p = sonyHeadphones();
        p.setName("Sony WH-1000XM5 Edición Especial");
        p.setDescription("Auriculares edición limitada con mejor batería");
        p.setUnitPrice(450.0f);
        p.setDiscount(0.10f);
        p.setStock(30);
        p.setBestSeller(false);
        return p;
    }

    // Desactivar producto
    public static ProductDTO sonyHeadphonesDeactivate() {
        ProductDTO p = sonyHeadphones();
        p.setActive(false);
        return p;
    }

    
    // CARTS E ITEMS
    
    public static CartItemDTO itemSony2Units() {
        return new CartItemDTO(1, 2, sonyHeadphones());
    }

    public static CartDTO cartWithSony() {
        return new CartDTO(1, 800.0f, List.of(itemSony2Units()));
    }

    // Agregar producto al carrito
    public static CartDTO cartAddAppleWatch() {
        CartItemDTO appleWatchItem = new CartItemDTO(
                2, 1,
                new ProductDTO(102, 1002, "Apple Watch Series 9", "Reloj inteligente",
                        700.0f, 0.05f, 30, List.of(2), 2, 4.8f,
                        List.of("https://example.com/watch.jpg"),
                        true, true, false, true, true)
        );

        return new CartDTO(1, 1500.0f, List.of(itemSony2Units(), appleWatchItem));
    }

    // Sacar producto del carrito
    public static CartDTO cartRemoveSony() {
        CartItemDTO appleWatchItem = new CartItemDTO(
                2, 1,
                new ProductDTO(102, 1002, "Apple Watch Series 9", "Reloj inteligente",
                        700.0f, 0.05f, 30, List.of(2), 2, 4.8f,
                        List.of("https://example.com/watch.jpg"),
                        true, true, false, true, true)
        );

        return new CartDTO(1, 700.0f, List.of(appleWatchItem));
    }

    
    // PURCHASES
    public static PurchaseWithCartDTO purchaseConfirmed() {
        return new PurchaseWithCartDTO(
                1,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                "Av. Siempre Viva 742",
                "CONFIRMED",
                cartWithSony()
        );
    }

    public static PurchaseWithCartDTO purchasePending() {
        return new PurchaseWithCartDTO(
                2,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(3),
                "Calle Falsa 123",
                "PENDING",
                cartAddAppleWatch()
        );
    }

    public static PurchaseWithCartDTO purchaseShipped() {
        return new PurchaseWithCartDTO(
                3,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "Gran Vía 100, Madrid",
                "SHIPPED",
                cartRemoveSony()
        );
    }
}
