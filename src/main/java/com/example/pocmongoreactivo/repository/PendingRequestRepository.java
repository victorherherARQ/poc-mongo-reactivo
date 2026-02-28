package com.example.pocmongoreactivo.repository;

import com.example.pocmongoreactivo.model.PendingRequest;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingRequestRepository extends ReactiveMongoRepository<PendingRequest, String> {
}
