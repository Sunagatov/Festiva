package com.festiva.friend.repository;

import com.festiva.friend.entity.Friend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MongoFriendRepository implements FriendRepository {

    private final FriendMongoRepository friendMongoRepository;

    @Override
    public void addFriend(long telegramUserId, Friend friend) {
        friend.setTelegramUserId(telegramUserId);
        friendMongoRepository.save(friend);
    }

    @Override
    public List<Friend> getFriends(long telegramUserId) {
        return friendMongoRepository.findByTelegramUserId(telegramUserId);
    }

    @Override
    public boolean friendExists(long telegramUserId, String name) {
        return friendMongoRepository.existsByTelegramUserIdAndName(telegramUserId, name);
    }

    @Override
    public void deleteFriend(long telegramUserId, String name) {
        friendMongoRepository.deleteByTelegramUserIdAndName(telegramUserId, name);
    }

    @Override
    public List<Long> getAllUserIds() {
        return friendMongoRepository.findAll().stream()
                .map(Friend::getTelegramUserId)
                .distinct()
                .toList();
    }
}
