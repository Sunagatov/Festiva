package com.festiva.friend.repository;

import com.festiva.friend.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findByTelegramUserId(long telegramUserId);
}
