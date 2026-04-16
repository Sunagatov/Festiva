package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class FriendService {

    public static final int FRIEND_CAP = 100;
    public static final int JUBILEE_INTERVAL = 5;
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

    public java.util.Optional<Friend> findFriendById(String id) {
        return friendRepository.findById(id);
    }

    public void deleteAllFriends(long telegramUserId) {
        friendRepository.deleteByTelegramUserId(telegramUserId);
    }

    public void updateFriendName(long telegramUserId, String oldName, String newName) {
        update(telegramUserId, oldName, f -> f.setName(newName));
    }

    public void updateFriendDate(long telegramUserId, String name, java.time.LocalDate date) {
        update(telegramUserId, name, f -> {
            f.setBirthYear(date.getYear());
            f.setBirthMonth(date.getMonthValue());
            f.setBirthDay(date.getDayOfMonth());
        });
    }
    
    public void updateFriendDate(long telegramUserId, String name, Integer year, int month, int day) {
        update(telegramUserId, name, f -> {
            // Validate before updating
            if (year != null) {
                java.time.LocalDate.of(year, month, day);
            } else {
                java.time.MonthDay.of(month, day);
            }
            f.setBirthYear(year);
            f.setBirthMonth(month);
            f.setBirthDay(day);
        });
    }

    public void updateFriendRelationship(long telegramUserId, String name, com.festiva.friend.entity.Relationship relationship) {
        update(telegramUserId, name, f -> f.setRelationship(relationship));
    }

    private void update(long telegramUserId, String name, Consumer<Friend> mutator) {
        friendRepository.findByTelegramUserIdAndNameIgnoreCase(telegramUserId, name)
                .ifPresent(f -> { mutator.accept(f); friendRepository.save(f); });
    }

    public boolean toggleFriendNotify(long telegramUserId, String name) {
        var ref = new Object() { boolean newValue = true; };
        friendRepository.findByTelegramUserIdAndNameIgnoreCase(telegramUserId, name)
                .ifPresent(f -> {
                    f.setNotifyEnabled(!f.isNotifyEnabled());
                    friendRepository.save(f);
                    ref.newValue = f.isNotifyEnabled();
                });
        return ref.newValue;
    }

    public boolean toggleFriendNotifyById(String id) {
        var ref = new Object() { boolean newValue = true; };
        friendRepository.findById(id)
                .ifPresent(f -> {
                    f.setNotifyEnabled(!f.isNotifyEnabled());
                    friendRepository.save(f);
                    ref.newValue = f.isNotifyEnabled();
                });
        return ref.newValue;
    }

    public List<Friend> getFriends(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId);
    }

    public List<Friend> getFriendsSortedByDayMonth(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> java.time.LocalDate.of(LEAP_YEAR, f.getBirthMonth(), f.getBirthDay())))
                .toList();
    }

    public List<Long> getAllUserIds() {
        return friendRepository.findDistinctTelegramUserIds();
    }

    public Map<Long, List<Friend>> getFriendsByUserIds(List<Long> userIds) {
        return friendRepository.findByTelegramUserIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Friend::getTelegramUserId));
    }
}
