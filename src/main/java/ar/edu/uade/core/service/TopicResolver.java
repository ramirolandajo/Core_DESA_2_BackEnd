package ar.edu.uade.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
public class TopicResolver {

    @Value("${app.kafka.topic.inventario}")
    private String inventarioTopic;
    @Value("${app.kafka.topic.ventas}")
    private String ventasTopic;
    @Value("${app.kafka.topic.notificar}")
    private String notificarTopic;

    public String resolveTopic(String type, String originModule){
        // 1) Si viene el originModule explícito, usarlo como guía
        if (originModule != null){
            String m = originModule.trim().toLowerCase();
            if (m.startsWith("vent")) return ventasTopic;
            if (m.startsWith("inven") || m.equals("storage-app")) return inventarioTopic;
            if (m.startsWith("notif")) return notificarTopic;
        }
        // 2) Si no, inferir por prefijo del type: <dominio>.<accion>
        if (type != null){
            String t = type.trim().toLowerCase();
            if (t.startsWith("ventas.")) return ventasTopic;
            if (t.startsWith("inventario.")) return inventarioTopic;
            if (t.startsWith("notificar.")) return notificarTopic;
        }
        // 3) fallback seguro
        return inventarioTopic;
    }

    public Set<String> getAllTopics(){
        Set<String> s = new HashSet<>();
        s.add(inventarioTopic);
        s.add(ventasTopic);
        s.add(notificarTopic);
        return s;
    }
}
