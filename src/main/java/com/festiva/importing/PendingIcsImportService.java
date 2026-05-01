package com.festiva.importing;

import com.festiva.friend.entity.Friend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PendingIcsImportService {

    private final PendingIcsImportRepository pendingIcsImportRepository;

    public void save(long userId, List<Friend> friends) {
        pendingIcsImportRepository.deleteByUserId(userId);
        if (friends != null && !friends.isEmpty()) {
            pendingIcsImportRepository.save(new PendingIcsImport(userId, friends));
        }
    }

    public List<Friend> get(long userId) {
        return pendingIcsImportRepository.findByUserId(userId)
                .map(PendingIcsImport::getFriends)
                .orElse(null);
    }

    public void delete(long userId) {
        pendingIcsImportRepository.deleteByUserId(userId);
    }
}
