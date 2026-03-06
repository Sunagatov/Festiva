package com.festiva.friend.repository;

import com.festiva.friend.entity.Friend;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

@SuppressWarnings("unused")
public interface FriendMongoRepository extends MongoRepository<Friend, String> {

    List<Friend> findByTelegramUserId(long telegramUserId);

    java.util.Optional<Friend> findByTelegramUserIdAndNameIgnoreCase(long telegramUserId, String name);

    boolean existsByTelegramUserIdAndNameIgnoreCase(long telegramUserId, String name);

    void deleteByTelegramUserIdAndNameIgnoreCase(long telegramUserId, String name);

    @Aggregation("{ $group: { _id: '$telegramUserId' } }")
    List<Long> findDistinctTelegramUserIds();
}
