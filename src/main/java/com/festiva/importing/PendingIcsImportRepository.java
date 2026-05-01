package com.festiva.importing;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PendingIcsImportRepository extends MongoRepository<PendingIcsImport, String> {

    Optional<PendingIcsImport> findByUserId(long userId);

    void deleteByUserId(long userId);
}
