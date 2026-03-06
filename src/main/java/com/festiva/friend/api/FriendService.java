package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int LEAP_YEAR = 2000; // leap year so Feb 29 sorts correctly

    private final FriendMongoRepository friendRepository;

    public void addFriend(long telegramUserId, Friend friend) {
        friend.setTelegramUserId(telegramUserId);
        friendRepository.save(friend);
    }

    public boolean friendExists(long telegramUserId, String name) {
        return friendRepository.existsByTelegramUserIdAndNameIgnoreCase(telegramUserId, name);
    }

    public void deleteFriend(long telegramUserId, String name) {
        friendRepository.deleteByTelegramUserIdAndNameIgnoreCase(telegramUserId, name);
    }

    public void updateFriendName(long telegramUserId, String oldName, String newName) {
        friendRepository.findByTelegramUserIdAndNameIgnoreCase(telegramUserId, oldName)
                .ifPresent(f -> { f.setName(newName); friendRepository.save(f); });
    }

    public void updateFriendDate(long telegramUserId, String name, java.time.LocalDate date) {
        friendRepository.findByTelegramUserIdAndNameIgnoreCase(telegramUserId, name)
                .ifPresent(f -> { f.setBirthDate(date); friendRepository.save(f); });
    }

    public List<Friend> getFriends(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId);
    }

    public List<Friend> getFriendsSortedByDayMonth(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> f.getBirthDate().withYear(LEAP_YEAR)))
                .toList();
    }

    public List<Long> getAllUserIds() {
        return friendRepository.findDistinctTelegramUserIds();
    }
}
