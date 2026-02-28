# PoC: Controller Imperativo + MongoDB Reactivo (Change Streams)

Microservicio Spring Boot donde un **controller imperativo** recibe una petición HTTP, crea un documento `PENDING` en MongoDB, y se queda **esperando reactivamente** (via Change Streams) a que dicho documento sea actualizado externamente.

## Arquitectura

```
HTTP POST /api/requests
       │
       ▼
  ┌─────────────┐    save()     ┌──────────────┐
  │  Controller  │ ────────────▶│   MongoDB     │
  │ (imperativo) │              │ (reactivo)    │
  │              │◀── watch ────│ Change Stream │
  └──────┬───────┘              └──────┬────────┘
         │                             ▲
         │ block()                     │ updateOne()
         │                             │
         ▼                      ┌──────┴────────┐
    HTTP Response               │  Script bash   │
                                │ update-mongo.sh│
                                └───────────────┘
```

## Requisitos

- Java 17+
- Docker y Docker Compose
- Maven (incluido via wrapper)

## Inicio rápido

### 1. Levantar MongoDB

```bash
docker compose up -d
```

Espera unos segundos a que el replica set se inicialice (el healthcheck lo hace automáticamente).

### 2. Instalar Maven Wrapper (si no existe)

```bash
mvn wrapper:wrapper
```

### 3. Compilar y ejecutar

```bash
./mvnw spring-boot:run
```

### 4. Hacer una petición (en otra terminal)

```bash
curl -X POST http://localhost:8080/api/requests
```

> La petición se quedará **bloqueada** esperando la actualización.

### 5. Actualizar el documento desde MongoDB (en otra terminal)

Copia el **ID** que aparece en los logs de la aplicación y ejecútalo:

```bash
./update-mongo.sh <REQUEST_ID>
```

> El `curl` debería recibir inmediatamente la respuesta con `status: COMPLETED`.

## Configuración

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/poc_reactive?replicaSet=rs0` | URI de conexión a MongoDB |
| `app.request.timeout-seconds` | `60` | Timeout en segundos para esperar la actualización |

## Estructura del proyecto

```
├── docker-compose.yml          # MongoDB 7 con replica set
├── update-mongo.sh             # Script para actualizar documento
├── pom.xml                     # Dependencias Maven
└── src/main/java/.../
    ├── PocMongoReactivoApplication.java
    ├── model/PendingRequest.java
    ├── repository/PendingRequestRepository.java
    ├── service/RequestService.java
    └── controller/RequestController.java
```
