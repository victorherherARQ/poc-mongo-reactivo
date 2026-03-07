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
69: 
70: ## Pruebas de Rendimiento
71: 
72: El proyecto incluye una suite de pruebas de carga con **k6**.
73: 
74: ### Ejecución
75: 
76: Para lanzar las pruebas parametrizadas y obtener un reporte en Markdown:
77: 
78: ```bash
79: ./performance/run-tests.sh
80: ```
81: 
82: El script solicitará:
83: - Número de hilos (VUs).
84: - Tiempo de rampa.
85: 
86: Los informes se guardarán automáticamente en `performance-reports/`.
87: 
88: > [!NOTE]
89: > Asegúrate de tener el contenedor de MongoDB y el auto-updater (`./performance/auto-updater.sh`) corriendo antes de iniciar los tests.
90: 

## Configuración

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/poc_reactive?replicaSet=rs0` | URI de conexión a MongoDB |
| `app.request.timeout-seconds` | `60` | Timeout en segundos para esperar la actualización |

## Estructura del proyecto

```
├── docker-compose.yml          # MongoDB 7 con replica set
├── update-mongo.sh             # Script para actualizar documento manual
├── pom.xml                     # Dependencias Maven
├── performance-reports/        # Reportes autogenerados (ignorado por git)
├── performance/                # Suite de pruebas de carga
│   ├── load-test.js            # Script base de k6
│   ├── auto-updater.sh         # Script para completar peticiones en bucle
│   └── run-tests.sh            # Lanzador interactivo
└── src/main/java/.../
    ├── PocMongoReactivoApplication.java
    ├── model/PendingRequest.java
    ├── repository/PendingRequestRepository.java
    ├── service/RequestService.java
    └── controller/RequestController.java
```
