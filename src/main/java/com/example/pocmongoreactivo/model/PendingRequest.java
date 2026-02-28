package com.example.pocmongoreactivo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Representa una solicitud pendiente de procesamiento en el sistema.
 * Esta entidad es almacenada en MongoDB y su estado es observado reactivamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pendingRequests")
public class PendingRequest {

    /**
     * Identificador único del documento en MongoDB.
     */
    @Id
    private String id;

    /**
     * Estado actual de la solicitud (ej: PENDING, COMPLETED).
     */
    @Builder.Default
    private String status = "PENDING";

    /**
     * Resultado del procesamiento externo, si aplica.
     */
    private String result;

    /**
     * Fecha y hora de creación del documento.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();
}
