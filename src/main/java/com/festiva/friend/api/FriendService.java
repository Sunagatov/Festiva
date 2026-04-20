package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    public static final int FRIEND_CAP = 100;
    public static final int JUBILEE_INTERVAL = 5;
    private static final int LEAP_YEAR = 2000;

    private final FriendMongoRepository friendRepository;

    public void addFriend(long telegramUserId, Friend friend) {
        if (friend == null) {
            throw new IllegalArgumentException("Friend cannot be null");
        }

        String sanitizedName = sanitizeName(friend.getName());
        friend.setTelegramUserId(telegramUserId);
        friend.setName(sanitizedName);

        try {
            friendRepository.save(friend);
        } catch (DuplicateKeyException e) {
            log.warn("friend.create.rejected.duplicate: userId={}", telegramUserId);
            throw new IllegalArgumentException("Friend with this name already exists", e);
        }
    }

    public boolean friendExists(long telegramUserId, String name) {
        return friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, Friend.normalizeName(name));
    }

    public void deleteFriend(long telegramUserId, String name) {
        friendRepository.deleteByTelegramUserIdAndNameIgnoreCase(telegramUserId, name);
    }

    public void deleteFriendById(String id, long telegramUserId) {
        friendRepository.deleteByIdAndTelegramUserId(id, telegramUserId);
    }

    public java.util.Optional<Friend> findOwnedFriend(String id, long telegramUserId) {
        return friendRepository.findByIdAndTelegramUserId(id, telegramUserId);
    }

    public void deleteAllFriends(long telegramUserId) {
        friendRepository.deleteByTelegramUserId(telegramUserId);
    }

    public void updateFriendNameById(String id, long telegramUserId, String newName) {
        String sanitizedName = sanitizeName(newName);
        findOwnedFriend(id, telegramUserId).ifPresent(friend -> {
            String newNormalized = Friend.normalizeName(sanitizedName);
            String currentNormalized = Friend.normalizeName(friend.getName());

            if (!newNormalized.equals(currentNormalized)
                    && friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, newNormalized)) {
                throw new IllegalArgumentException("Friend with this name already exists");
            }

            friend.setName(sanitizedName);
            try {
                friendRepository.save(friend);
            } catch (DuplicateKeyException e) {
                log.warn("friend.update.rejected.duplicate: userId={}, friendId={}", telegramUserId, id);
                throw new IllegalArgumentException("Friend with this name already exists", e);
            }
        });
    }

    public void updateFriendDateById(String id, long telegramUserId, Integer year, int month, int day) {
        var existing = findOwnedFriend(id, telegramUserId);
        if (existing.isEmpty()) {
            return;
        }

        try {
            if (year != null) {
                @SuppressWarnings("unused")
                java.time.LocalDate validDate = java.time.LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                java.time.MonthDay validMonthDay = java.time.MonthDay.of(month, day);
            }
        } catch (java.time.DateTimeException e) {
            log.warn("friend.date.update.rejected.invalid: userId={}, friendId={}, hasYear={}",
                    telegramUserId, id, year != null, e);
            throw new IllegalArgumentException("Invalid date: " +
                    (year != null ? year + "-" : "") + month + "-" + day, e);
        }

        Friend friend = existing.get();
        friend.setBirthYear(year);
        friend.setBirthMonth(month);
        friend.setBirthDay(day);
        friendRepository.save(friend);
    }

    public void updateFriendRelationshipById(String id, long telegramUserId, com.festiva.friend.entity.Relationship relationship) {
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
            f.setRelationship(relationship);
            friendRepository.save(f);
        });
    }

    public boolean toggleFriendNotifyById(String id, long telegramUserId) {
        var ref = new Object() { boolean newValue = true; };
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
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

    private String sanitizeName(String name) {
        String sanitized = name == null ? null : name.trim();
        if (sanitized == null || sanitized.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }
        return sanitized;
    }
}
