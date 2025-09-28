package ar.edu.uade.core.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class TopicResolver {

    private final Map<String, String> byType = new HashMap<>();
    private final String fallback = "core.events";

    public TopicResolver(){
        // Inventario
        byType.put("PUT: Actualizar stock", "inventario.actualizar-stock");
        byType.put("POST: Agregar un producto", "inventario.producto-agregado");
        byType.put("PUT: Modificar producto", "inventario.producto-modificado");
        byType.put("PATCH: Producto desactivado", "inventario.producto-desactivado");
        byType.put("PATCH: Producto activado", "inventario.producto-activado");
        byType.put("PUT: Producto actualizado", "inventario.producto-actualizado");
        byType.put("POST: Marca creada", "inventario.marca-creada");
        byType.put("PATCH: Marca desactivada", "inventario.marca-desactivada");
        byType.put("POST: Categoría creada", "inventario.categoria-creada");
        byType.put("POST: Categoria creada", "inventario.categoria-creada");
        byType.put("PATCH: Categoría desactivada", "inventario.categoria-desactivada");
        byType.put("PATCH: Categoria desactivada", "inventario.categoria-desactivada");
        byType.put("POST: Stock rollback - compra cancelada", "inventario.stock-rollback");

        // Ventas
        byType.put("POST: Compra pendiente", "ventas.compra-pendiente");
        byType.put("POST: Compra confirmada", "ventas.compra-confirmada");
        byType.put("DELETE: Compra cancelada", "ventas.compra-cancelada");
        byType.put("POST: Review creada", "ventas.review-creada");
        byType.put("POST: Producto agregado a favoritos", "ventas.favorito-agregado");
        byType.put("DELETE: Producto quitado de favoritos", "ventas.favorito-quitado");

        // Analítica / consultas
        byType.put("GET: Vista diaria de productos", "analitica.vista-diaria-productos");
    }

    public String resolveTopicForType(String type){
        if (type == null) return fallback;
        String topic = byType.get(type);
        if (topic != null) return topic;
        // fallback: normalizar mínimamente
        String key = type.toLowerCase().replace(' ', '-').replace(':', '-');
        return "core." + key;
    }

    public Set<String> getAllTopics(){
        Set<String> s = new HashSet<>(byType.values());
        s.add(fallback);
        return s;
    }
}

