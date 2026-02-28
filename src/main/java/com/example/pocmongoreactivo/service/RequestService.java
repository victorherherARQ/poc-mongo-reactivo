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

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final PendingRequestRepository repository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    /**
     * Crea un documento PENDING en MongoDB y se suscribe al Change Stream
     * para esperar a que sea actualizado externamente.
     *
     * @param timeoutSeconds tiempo máximo de espera en segundos
     * @return el documento actualizado, o lanza error si se agota el timeout
     */
    public PendingRequest createAndWaitForUpdate(int timeoutSeconds) {
        // 1. Insertar documento con estado PENDING (reactivo, bloqueamos para obtener el ID)
        PendingRequest saved = repository.save(PendingRequest.builder().build())
                .block(Duration.ofSeconds(5));

        if (saved == null) {
            throw new RuntimeException("No se pudo insertar el documento en MongoDB");
        }

        log.info("Documento creado con ID: {} - Escuchando todos los cambios en 'pendingRequests'...", saved.getId());

        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .returnFullDocumentOnUpdate()
                .build();

        // 3. Escuchar el Change Stream sin filtros de agregación (más robusto para debug/PoC)
        return reactiveMongoTemplate
                .changeStream("pendingRequests", options, PendingRequest.class)
                .doOnSubscribe(s -> log.info(">>> SUSCRITO AL CHANGE STREAM - Esperando eventos..."))
                .map(event -> {
                    log.info("Cambio detectado: operationType={}, status={}, body={}", 
                            event.getOperationType(), 
                            event.getBody() != null ? event.getBody().getStatus() : "null",
                            event.getBody());
                    return event.getBody();
                })
                // Filtramos por ID y estado COMPLETED en el flujo reactivo
                .filter(doc -> doc != null && 
                               saved.getId().equals(doc.getId()) && 
                               "COMPLETED".equals(doc.getStatus()))
                .next()
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("Error/Timeout para ID {}: {}", saved.getId(), e.getMessage()))
                .block();
    }
}
