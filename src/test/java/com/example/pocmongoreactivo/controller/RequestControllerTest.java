package com.example.pocmongoreactivo.controller;

import com.example.pocmongoreactivo.model.PendingRequest;
import com.example.pocmongoreactivo.repository.PendingRequestRepository;
import com.example.pocmongoreactivo.service.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para {@link RequestController}.
 * Utiliza MockMvc para simular peticiones HTTP y MockBean para los servicios dependientes.
 */
@WebMvcTest(RequestController.class)
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @MockBean
    private PendingRequestRepository repository;

    private PendingRequest pendingRequest;

    @BeforeEach
    void setUp() {
        pendingRequest = PendingRequest.builder()
                .id("test-id")
                .status("COMPLETED")
                .result("Success")
                .build();
    }

    /**
     * Verifica que el endpoint POST /api/requests responda correctamente
     * cuando el flujo se completa con éxito.
     */
    @Test
    void createRequest_Success() throws Exception {
        // Mock de la inserción inicial
        when(repository.save(any(PendingRequest.class)))
                .thenReturn(Mono.just(PendingRequest.builder().id("test-id").build()));

        // Mock de la espera del servicio
        when(requestService.waitForUpdate(eq("test-id"), anyInt()))
                .thenReturn(pendingRequest);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /**
     * Verifica que el endpoint responda con 408 Timeout cuando la actualización
     * excede el tiempo límite definido.
     */
    @Test
    void createRequest_Timeout() throws Exception {
        when(repository.save(any(PendingRequest.class)))
                .thenReturn(Mono.just(PendingRequest.builder().id("timeout-id").build()));

        // Simulamos un timeout lanzando una excepción que el controlador maneja
        when(requestService.waitForUpdate(eq("timeout-id"), anyInt()))
                .thenThrow(new RuntimeException(new java.util.concurrent.TimeoutException("Wait timed out")));

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("$.error").value("Timeout"));
    }
}
