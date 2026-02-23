package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;

    public void addFriend(long telegramUserId, Friend friend) {
        friendRepository.addFriend(telegramUserId, friend);
    }

    public boolean friendExists(long telegramUserId, String name) {
        return friendRepository.friendExists(telegramUserId, name);
    }

    public void deleteFriend(long telegramUserId, String name) {
        friendRepository.deleteFriend(telegramUserId, name);
    }

    public List<Friend> getFriendsSortedByDayMonth(long telegramUserId) {
        return friendRepository.getFriends(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> f.getBirthDate().withYear(2000)))
                .toList();
    }

    public List<Friend> getFriendsSortedByUpcomingBirthday(long telegramUserId) {
        LocalDate today = LocalDate.now();
        return friendRepository.getFriends(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> nextBirthday(f.getBirthDate(), today)))
                .toList();
    }

    public List<Long> getAllUserIds() {
        return friendRepository.getAllUserIds();
    }

    public List<Friend> getFriends(long telegramUserId) {
        return friendRepository.getFriends(telegramUserId);
    }

    public static LocalDate nextBirthday(LocalDate birthDate, LocalDate from) {
        LocalDate next = birthDate.withYear(from.getYear());
        return next.isBefore(from) ? next.plusYears(1) : next;
    }
}
