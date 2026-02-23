package com.festiva.friend.repository;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MongoFriendRepository implements FriendRepository {

    private final UserMongoRepository userMongoRepository;

    @Override
    public void addFriend(long telegramUserId, Friend friend) {
        UserEntity user = userMongoRepository.findByTelegramUserId(telegramUserId)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setTelegramUserId(telegramUserId);
                    return newUser;
                });
        user.getFriends().add(friend);
        userMongoRepository.save(user);
    }

    @Override
    public List<Friend> getFriends(long telegramUserId) {
        return userMongoRepository.findByTelegramUserId(telegramUserId)
                .map(UserEntity::getFriends)
                .orElse(List.of());
    }

    @Override
    public boolean friendExists(long telegramUserId, String name) {
        return getFriends(telegramUserId).stream()
                .anyMatch(f -> f.getName().equals(name));
    }

    @Override
    public void deleteFriend(long telegramUserId, String name) {
        userMongoRepository.findByTelegramUserId(telegramUserId)
                .ifPresent(user -> {
                    user.getFriends().removeIf(f -> f.getName().equals(name));
                    userMongoRepository.save(user);
                });
    }

    @Override
    public List<Long> getAllUserIds() {
        return userMongoRepository.findAll().stream()
                .map(UserEntity::getTelegramUserId)
                .toList();
    }
}
