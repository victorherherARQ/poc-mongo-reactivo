package com.example.pocmongoreactivo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pendingRequests")
public class PendingRequest {

    @Id
    private String id;

    @Builder.Default
    private String status = "PENDING";

    private String result;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
