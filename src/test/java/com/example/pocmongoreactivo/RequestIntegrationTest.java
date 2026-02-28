package com.example.pocmongoreactivo;

import com.example.pocmongoreactivo.model.PendingRequest;
import com.example.pocmongoreactivo.repository.PendingRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Prueba de integración de extremo a extremo (E2E).
 * Utiliza Testcontainers para levantar una instancia real de MongoDB en Docker.
 * NOTA: Los Change Streams requieren que MongoDB esté en modo Replica Set.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RequestIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PendingRequestRepository repository;

    /**
     * Prueba el flujo completo:
     * 1. Realiza POST al controlador.
     * 2. El controlador se bloquea.
     * 3. Un hilo separado actualiza el documento en Mongo.
     * 4. El controlador detecta el cambio y responde.
     */
    @Test
    void fullRequestProcess_Success() throws Exception {
        // Lanzamos la petición en un hilo separado para que no bloquee el test
        CompletableFuture<ResponseEntity<PendingRequest>> futureResponse = CompletableFuture.supplyAsync(() -> 
            restTemplate.postForEntity("/api/requests", null, PendingRequest.class)
        );

        // Esperamos un momento a que el documento se cree en la DB
        Thread.sleep(2000);

        // Buscamos el documento creado (debería haber solo uno)
        PendingRequest created = repository.findAll().blockFirst();
        assertNotNull(created, "El documento debería haber sido creado");
        assertEquals("PENDING", created.getStatus());

        // Simulamos la actualización externa (como haría el script)
        created.setStatus("COMPLETED");
        created.setResult("Integración OK");
        repository.save(created).block();

        // Obtenemos la respuesta del controlador (que debería haberse desbloqueado)
        ResponseEntity<PendingRequest> response = futureResponse.get(10, TimeUnit.SECONDS);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("COMPLETED", response.getBody().getStatus());
        assertEquals("Integración OK", response.getBody().getResult());
    }
}
