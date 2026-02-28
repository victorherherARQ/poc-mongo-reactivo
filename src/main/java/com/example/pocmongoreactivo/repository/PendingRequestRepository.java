package com.example.pocmongoreactivo.repository;

import com.example.pocmongoreactivo.model.PendingRequest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo para la entidad {@link PendingRequest}.
 * Proporciona operaciones CRUD asíncronas sobre la colección 'pendingRequests' de MongoDB.
 */
@Repository
public interface PendingRequestRepository extends ReactiveMongoRepository<PendingRequest, String> {
}
