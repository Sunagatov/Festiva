package com.festiva.user.api;

import com.festiva.user.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class UserDateService {

    private final UserPreferenceService userPreferenceService;

    public LocalDate todayFor(long userId) {
        String timezone = userPreferenceService.getTimezone(userId);
        String effectiveTimezone = (timezone == null || timezone.isBlank())
                ? UserPreference.DEFAULT_TIMEZONE
                : timezone;

        try {
            return LocalDate.now(ZoneId.of(effectiveTimezone));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of(UserPreference.DEFAULT_TIMEZONE));
        }
    }
}
