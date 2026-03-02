package com.festiva.health;

import com.festiva.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final FriendRepository friendRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            friendRepository.count();
            return ResponseEntity.ok(Map.of("status", "UP", "database", "connected"));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "disconnected"));
        }
    }
}
