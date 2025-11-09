package ar.edu.uade.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TopicResolverTest {

    private TopicResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TopicResolver();
        ReflectionTestUtils.setField(resolver, "inventarioTopic", "inv-topic");
        ReflectionTestUtils.setField(resolver, "ventasTopic", "ven-topic");
        ReflectionTestUtils.setField(resolver, "notificarTopic", "noti-topic");
    }

    @Test
    void resolveTopic_UsesOriginModule_First() {
        String topic = resolver.resolveTopic("inventario.algo", "VentasBackoffice");
        assertEquals("ven-topic", topic, "Debe priorizar originModule que empieza con 'vent'");

        topic = resolver.resolveTopic("ventas.creada", "storage-app");
        assertEquals("inv-topic", topic, "'storage-app' mapea a inventario");

        topic = resolver.resolveTopic("ventas.creada", "NotificacionesSrv");
        assertEquals("noti-topic", topic, "Origin 'noti' mapea a notificar");
    }

    @Test
    void resolveTopic_WhenOriginNull_InfersFromTypePrefix() {
        assertEquals("ven-topic", resolver.resolveTopic("ventas.created", null));
        assertEquals("inv-topic", resolver.resolveTopic("inventario.update", null));
        assertEquals("noti-topic", resolver.resolveTopic("notificar.push", null));
    }

    @Test
    void resolveTopic_FallbacksToInventario() {
        assertEquals("inv-topic", resolver.resolveTopic(null, null));
        assertEquals("inv-topic", resolver.resolveTopic(" ", " "));
    }

    @Test
    void resolveTopic_OriginBeatsType_WhenBothProvided() {
        String topic = resolver.resolveTopic("inventario.ajuste", "Notificaciones");
        assertEquals("noti-topic", topic, "Origin 'Notificaciones' debe ganar sobre type 'inventario.*'");
    }

    @Test
    void getAllTopics_ReturnsThreeUnique() {
        Set<String> all = resolver.getAllTopics();
        assertEquals(3, all.size());
        assertTrue(all.contains("inv-topic"));
        assertTrue(all.contains("ven-topic"));
        assertTrue(all.contains("noti-topic"));
    }
}

