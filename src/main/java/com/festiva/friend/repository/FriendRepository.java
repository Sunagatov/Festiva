package com.festiva.friend.repository;

import com.festiva.friend.entity.Friend;

import java.util.List;

public interface FriendRepository {

    void addFriend(long telegramUserId, Friend friend);

    List<Friend> getFriends(long telegramUserId);

    boolean friendExists(long telegramUserId, String name);

    void deleteFriend(long telegramUserId, String name);

    List<Long> getAllUserIds();
}
