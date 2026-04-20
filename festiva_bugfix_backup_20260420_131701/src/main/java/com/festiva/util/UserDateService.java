package com.festiva.util;

import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserDateService {

    private final UserStateService userStateService;

    public LocalDate todayFor(long userId) {
        String timezone = userStateService.getTimezone(userId);
        try {
            return LocalDate.now(ZoneId.of(timezone));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
