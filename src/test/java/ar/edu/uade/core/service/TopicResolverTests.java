package ar.edu.uade.core.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TopicResolverTests {

    private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void resolveTopic_ByOriginModule() throws Exception {
        TopicResolver r = new TopicResolver();
        setPrivate(r, "inventarioTopic", "tInv");
        setPrivate(r, "ventasTopic", "tVen");
        setPrivate(r, "notificarTopic", "tNot");

        assertEquals("tVen", r.resolveTopic("algo", "ventas-app"));
        assertEquals("tInv", r.resolveTopic("algo", "inventario"));
        assertEquals("tNot", r.resolveTopic("algo", "notifier"));
    }

    @Test
    void resolveTopic_ByTypePrefix_AndFallback() throws Exception {
        TopicResolver r = new TopicResolver();
        setPrivate(r, "inventarioTopic", "tInv");
        setPrivate(r, "ventasTopic", "tVen");
        setPrivate(r, "notificarTopic", "tNot");

        assertEquals("tVen", r.resolveTopic("ventas.alta", null));
        assertEquals("tInv", r.resolveTopic("inventario.baja", null));
        assertEquals("tNot", r.resolveTopic("notificar.push", null));
        assertEquals("tInv", r.resolveTopic(null, null));
    }
}
