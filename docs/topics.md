Convención de nombres de tópicos Kafka
- Formato: <dominio>.<accion>
- Dominios: inventario.*, ventas.*, analitica.*
- Ejemplos: inventario.producto-creado, ventas.compra-confirmada, analitica.review-creada

Lista de tópicos por dominio
- Inventario:
  - inventario.producto-creado
  - inventario.producto-actualizado
  - inventario.producto-modificado
  - inventario.producto-activado
  - inventario.producto-desactivado
  - inventario.stock-actualizado
  - inventario.stock-rollback
  - inventario.marca-creada
  - inventario.marca-desactivada
  - inventario.categoria-creada
  - inventario.categoria-desactivada
- Ventas:
  - ventas.compra-pendiente
  - ventas.compra-confirmada
  - ventas.compra-cancelada
  - ventas.review-creada
  - ventas.favorito-agregado
  - ventas.favorito-quitado
- Analítica:
  - analitica.vista-diaria-productos

Tabla de mapeo: Event Type (original) → Tópico Kafka → Consumidores esperados
1) PUT: Actualizar stock → inventario.stock-actualizado → Analítica, Ventas
2) POST: Agregar un producto → inventario.producto-creado → Analítica, Ventas
3) PUT: Modificar producto → inventario.producto-modificado → Analítica, Ventas
4) PATCH: Producto desactivado → inventario.producto-desactivado → Analítica, Ventas
5) PATCH: Producto activado → inventario.producto-activado → Analítica, Ventas
6) PUT: Producto actualizado → inventario.producto-actualizado → Analítica, Ventas
7) POST: Marca creada → inventario.marca-creada → Analítica, Ventas
8) POST: Categoría/Categoria creada → inventario.categoria-creada → Analítica, Ventas
9) PATCH: Marca desactivada → inventario.marca-desactivada → Analítica, Ventas
10) PATCH: Categoría/Categoria desactivada → inventario.categoria-desactivada → Analítica, Ventas
11) POST: Compra pendiente → ventas.compra-pendiente → Inventario
12) POST: Compra confirmada → ventas.compra-confirmada → Inventario, Analítica
13) DELETE: Compra cancelada → ventas.compra-cancelada → Inventario
14) POST: Review creada → ventas.review-creada → Analítica
15) POST: Producto agregado a favoritos → ventas.favorito-agregado → Analítica
16) DELETE: Producto quitado de favoritos → ventas.favorito-quitado → Analítica
17) GET: Vista diaria de productos → analitica.vista-diaria-productos → Analítica
18) POST: Stock rollback - compra cancelada → inventario.stock-rollback → Inventario

Contrato HTTP (Middleware → Core)
- URL (host): POST http://localhost:8082/api/core/events
- URL (intra-docker core-net): POST http://core-service:8082/api/core/events
- Headers: Content-Type: application/json
- Body (EventRequest):
  - type: string (obligatorio). Debe ser el event type "original" de la tabla (ej.: "POST: Compra confirmada").
  - payload: objeto JSON (obligatorio). Contenido específico del evento.
  - timestamp: string ISO-8601 (opcional). Si no se envía, el Core lo setea.
  - originModule: string (obligatorio). Módulo funcional de origen (p. ej., "ventas", "inventario"). Evitar usar "Middleware" aquí.

Publicación en Kafka (payload estandarizado)
- El Core convierte el event type original al tópico Kafka según la tabla y publica un mensaje con el siguiente esquema:
  {
    "eventId": "UUID",
    "eventType": "<dominio>.<accion>",
    "timestamp": "ISO-8601 (UTC)",
    "originModule": "<modulo-origen>",
    "payload": { ... }
  }
- Ejemplo para "POST: Compra confirmada":
  {
    "eventId": "f8b5d8d8-4c6a-4b2a-8b35-2f9c2f9c2f9c",
    "eventType": "ventas.compra-confirmada",
    "timestamp": "2025-09-28T20:45:00Z",
    "originModule": "ventas",
    "payload": {
      "compraId": "C-12345",
      "clienteId": "U-987",
      "items": [
        {"sku": "SKU-001", "cantidad": 2, "precioUnitario": 1500.0},
        {"sku": "SKU-002", "cantidad": 1, "precioUnitario": 3499.99}
      ],
      "total": 6499.99,
      "moneda": "ARS"
    }
  }

Notas de despliegue (Docker)
- Broker Kafka expuesto en host: localhost:9092
- Kafka UI: http://localhost:9094 (cluster: kafka:19092)
- Base de datos MySQL: localhost:3306 (contendor: mysql:3306), DB: core_dev, user: core, pass: corepwd
- El servicio Core en Docker consume Kafka en kafka:19092 y MySQL en mysql:3306; si se corre en host, usa localhost por defecto.

