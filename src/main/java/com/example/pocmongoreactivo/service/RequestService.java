package com.example.pocmongoreactivo.service;

import com.example.pocmongoreactivo.model.PendingRequest;
import com.example.pocmongoreactivo.repository.PendingRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

/**
 * Servicio encargado de la lógica de espera reactiva utilizando MongoDB Change Streams.
 * Proporciona mecanismos para suscribirse a cambios en tiempo real sobre la base de datos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final PendingRequestRepository repository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    /**
     * Se suscribe al Change Stream para esperar a que un documento específico 
     * sea actualizado a COMPLETED.
     *
     * @param requestId ID del documento a observar
     * @param timeoutSeconds tiempo máximo de espera
     * @return el documento actualizado
     */
    public PendingRequest waitForUpdate(String requestId, int timeoutSeconds) {
        log.info("Iniciando espera reactiva para ID: {} (timeout: {}s)", requestId, timeoutSeconds);

        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .returnFullDocumentOnUpdate()
                .build();

        // Escuchar el Change Stream filtrando por el ID y el estado COMPLETED
        return reactiveMongoTemplate
                .changeStream("pendingRequests", options, PendingRequest.class)
                .doOnSubscribe(s -> log.info(">>> SUSCRITO AL CHANGE STREAM - Esperando evento para ID: {}", requestId))
                .map(event -> {
                    log.info("Cambio detectado en la colección: type={}, status={}, bodyId={}", 
                            event.getOperationType(), 
                            event.getBody() != null ? event.getBody().getStatus() : "null",
                            event.getBody() != null ? event.getBody().getId() : "null");
                    return event.getBody();
                })
                .filter(doc -> doc != null && 
                               requestId.equals(doc.getId()) && 
                               "COMPLETED".equals(doc.getStatus()))
                .next()
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("Error/Timeout esperando ID {}: {}", requestId, e.getMessage()))
                .block();
    }
}
