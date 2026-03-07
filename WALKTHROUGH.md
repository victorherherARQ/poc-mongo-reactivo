# PoC: Controller Imperativo + MongoDB Reactivo (Change Streams)

Esta PoC demuestra cómo implementar un microservicio con **Spring Boot** que utiliza un **controlador REST imperativo** (Spring MVC) para esperar de forma eficiente y **reactiva** a que un documento sea actualizado en **MongoDB** por un proceso externo.

## Logros

- [x] **Arquitectura Híbrida**: Integración de un flujo imperativo (Web) con un flujo reactivo (MongoDB Change Streams).
- [x] **Suscripción Reactiva**: Uso de `ReactiveMongoTemplate.changeStream()` para observar cambios en tiempo real sin polling.
- [x] **Bloqueo Inteligente**: El controlador bloquea el hilo de ejecución con `.block(timeout)` mientras el driver de Mongo escucha el evento de forma asíncrona.
- [x] **Gestión de Timeouts**: Implementación de un timeout de 60 segundos con respuesta HTTP 408 si no hay acción externa.
- [x] **Infraestructura con Docker**: Configuración automática de MongoDB 7 con Replica Set (necesario para Change Streams).

## Cómo funciona

1. El cliente envía un `POST /api/requests`.
2. El servicio crea un documento en estado `PENDING` y obtiene su ID.
3. Se abre un **Change Stream** en MongoDB filtrado por ese ID.
4. El hilo del servidor se bloquea esperando un evento donde el estado cambie a `COMPLETED`.
5. Un script externo (o comando de base de datos) actualiza el documento.
6. MongoDB notifica al driver, el `Flux` emite el documento actualizado, y el controlador recibe el valor para responder al cliente.

## Código Principal

### Servicio Reactivo

En [RequestService.java](file:///home/vhdez/agentes/poc-mongo-reactivo/src/main/java/com/example/pocmongoreactivo/service/RequestService.java) vemos el núcleo de la lógica:

```java
public PendingRequest createAndWaitForUpdate(int timeoutSeconds) {
    // 1. Insertar documento PENDING
    PendingRequest saved = repository.save(PendingRequest.builder().build()).block();

    // 2. Escuchar el Change Stream reactivamente
    return reactiveMongoTemplate
            .changeStream("pendingRequests", options, PendingRequest.class)
            .filter(doc -> saved.getId().equals(doc.getId()) && "COMPLETED".equals(doc.getStatus()))
            .next() // Tomar el primer evento que cumpla el filtro
            .timeout(Duration.ofSeconds(timeoutSeconds)) // Gestionar el tiempo de espera
            .block(); // Bloquear el hilo imperativo hasta recibir el evento
}
```

## Verificación Visual

### 1. Petición bloqueada (Estado PENDING)
Al realizar el `curl`, la terminal se queda en espera:
```bash
$ curl -X POST http://localhost:8080/api/requests
```

### 2. Actualización externa
Ejecutamos el script proporcionado con el ID mostrado en los logs:
```bash
$ ./update-mongo.sh 69a35b68ce3c7a2e53d9fb11
✅ Documento actualizado. El controller debería responder ahora.
```

### 3. Respuesta recibida
Inmediatamente, el `curl` que estaba esperando recibe la respuesta:
```json
{
  "id": "69a360100e3c387720f2c0bf",
  "status": "COMPLETED",
  "result": "Respuesta procesada externamente desde script",
  "createdAt": "2026-02-28T21:37:20.035Z"
}
```

## Pruebas Automatizadas

La PoC incluye una suite de pruebas completa:

1. **Pruebas Unitarias**:
   - `RequestControllerTest`: Verifica el comportamiento del endpoint y el manejo de timeouts.
   - `RequestServiceTest`: Valida el filtrado reactivo de los Change Streams.
2. **Pruebas de Integración**:
   - `RequestIntegrationTest`: Utiliza **Testcontainers** para levantar un MongoDB real en Docker y validar el flujo E2E completo de forma automatizada.

Para ejecutar las pruebas:
```bash
mvn test
```

## Pruebas de Rendimiento (Load Testing)

El sistema se ha validado bajo carga para entender su comportamiento ante la concurrencia:

- **Herramienta**: [k6](https://k6.io/) para la generación de carga parametrizable.
- **Escenario**: 50 usuarios concurrentes con rampa de subida y bajada.
- **Automatización**: Dashboards e informes en Markdown generados tras cada ejecución.
- **Cuello de Botella Identificado**: Tomcat Thread Pool (200 hilos por defecto). Cada petición bloquea un hilo mientras espera la base de datos.

Para lanzar una prueba:
```bash
./performance/run-tests.sh
```

## Configuración de Infraestructura

El `docker-compose.yml` incluye un healthcheck que inicializa automáticamente el **Replica Set** (`rs0`), permitiendo el uso de Change Streams sin configuración manual adicional.
