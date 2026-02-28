package com.example.pocmongoreactivo.controller;

import com.example.pocmongoreactivo.model.PendingRequest;
import com.example.pocmongoreactivo.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final com.example.pocmongoreactivo.repository.PendingRequestRepository repository;

    @Value("${app.request.timeout-seconds:60}")
    private int timeoutSeconds;

    /**
     * Endpoint imperativo que crea un documento PENDING y se queda bloqueado
     * esperando a que sea actualizado externamente vía Change Stream.
     */
    @PostMapping
    public ResponseEntity<?> createRequest() {
        log.info("Petición recibida - Creando documento y esperando actualización (timeout: {}s)...", timeoutSeconds);

        try {
            // 1. El controlador usa el repositorio directamente para crear el documento PENDING
            PendingRequest pending = PendingRequest.builder().build();
            PendingRequest saved = repository.save(pending).block(java.time.Duration.ofSeconds(5));
            
            log.info("Documento PENDING creado con ID: {}. Esperando actualización externa...", saved.getId());

            // 2. Llama al servicio para esperar reactivamente
            PendingRequest result = requestService.waitForUpdate(saved.getId(), timeoutSeconds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.warn("Timeout alcanzado esperando la actualización del documento");
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                        .body(Map.of(
                                "error", "Timeout",
                                "message", "No se recibió actualización en " + timeoutSeconds + " segundos"
                        ));
            }
            log.error("Error inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isTimeoutException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof TimeoutException || cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
