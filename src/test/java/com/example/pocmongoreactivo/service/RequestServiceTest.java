package com.example.pocmongoreactivo.service;

import com.example.pocmongoreactivo.model.PendingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link RequestService}.
 * Verifica la lógica de filtrado del Change Stream usando StepVerifier de Reactor.
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @InjectMocks
    private RequestService requestService;

    /**
     * Prueba que el servicio detecta correctamente un evento de actualización 
     * para el ID solicitado cuando el estado cambia a COMPLETED.
     */
    @Test
    void waitForUpdate_Success() {
        String requestId = "target-id";
        PendingRequest targetDoc = PendingRequest.builder()
                .id(requestId)
                .status("COMPLETED")
                .build();

        // Simulamos un evento del Change Stream
        @SuppressWarnings("unchecked")
        ChangeStreamEvent<PendingRequest> event = mock(ChangeStreamEvent.class);
        when(event.getBody()).thenReturn(targetDoc);

        // Configuramos el mock de template para devolver el flujo simulado
        when(reactiveMongoTemplate.changeStream(eq("pendingRequests"), any(), eq(PendingRequest.class)))
                .thenReturn(Flux.just(event));

        // Ejecutamos y verificamos de forma imperativa (como hace el controlador con .block())
        PendingRequest result = requestService.waitForUpdate(requestId, 5);

        assertEquals(requestId, result.getId());
        assertEquals("COMPLETED", result.getStatus());
    }
}
