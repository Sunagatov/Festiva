package com.festiva.friend.repository;

import com.festiva.friend.entity.Friend;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FriendMongoRepository extends MongoRepository<Friend, String> {

    List<Friend> findByTelegramUserId(long telegramUserId);

    boolean existsByTelegramUserIdAndName(long telegramUserId, String name);

    void deleteByTelegramUserIdAndName(long telegramUserId, String name);
}
