package com.festiva.user.api;

import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;

    public Lang getLanguage(long userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::getLang)
                .orElse(UserPreference.DEFAULT_LANG);
    }

    public void setLanguage(long userId, Lang lang) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setLang(lang);
        userPreferenceRepository.save(pref);
    }

    public int getNotifyHour(long userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::getNotifyHour)
                .orElse(9);
    }

    public void setNotifyHour(long userId, int hour) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setNotifyHour(hour);
        userPreferenceRepository.save(pref);
    }

    public String getTimezone(long userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::getTimezone)
                .orElse(UserPreference.DEFAULT_TIMEZONE);
    }

    public void setTimezone(long userId, String timezone) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setTimezone(timezone);
        userPreferenceRepository.save(pref);
    }

    public Map<Long, UserPreference> findByUserIds(List<Long> userIds) {
        return userPreferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserPreference::getTelegramUserId, pref -> pref));
    }

    public void markLastNotifiedDate(long userId, LocalDate date) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setLastNotifiedDate(date);
        userPreferenceRepository.save(pref);
    }

    public void deletePreferences(long userId) {
        userPreferenceRepository.deleteById(userId);
    }

    private UserPreference getOrCreatePref(long userId) {
        return userPreferenceRepository.findById(userId)
                .orElse(new UserPreference(userId, UserPreference.DEFAULT_LANG, 9, UserPreference.DEFAULT_TIMEZONE, null));
    }
}
