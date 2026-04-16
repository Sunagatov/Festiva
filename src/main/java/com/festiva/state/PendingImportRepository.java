package com.festiva.state;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PendingImportRepository extends MongoRepository<PendingImport, String> {

    Optional<PendingImport> findByUserId(long userId);

    void deleteByUserId(long userId);
}
