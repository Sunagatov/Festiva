package com.festiva.user.api;

import com.festiva.friend.api.FriendService;
import com.festiva.importing.PendingIcsImportService;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class AccountDeletionService {

    private final FriendService friendService;
    private final UserPreferenceService userPreferenceService;
    private final PendingIcsImportService pendingIcsImportService;
    private final UserStateService userStateService;

    public void deleteAccount(long userId) {
        friendService.deleteAllFriends(userId);
        userPreferenceService.deletePreferences(userId);
        pendingIcsImportService.delete(userId);
        userStateService.removeSession(userId);
    }
}
