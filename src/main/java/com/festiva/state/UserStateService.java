package com.festiva.state;

import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserStateService {

    private static class UserSession {
        BotState state = BotState.IDLE;
        String pendingName = null;
        Integer pendingYear = null;
        Integer pendingMonth = null;
        Integer pendingDay = null;
        int yearPageOffset = 0;
        Lang lang = null;
    }

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final UserPreferenceRepository userPreferenceRepository;

    private UserSession session(long userId) {
        return sessions.computeIfAbsent(userId, k -> new UserSession());
    }

    public BotState getState(long userId) { return session(userId).state; }
    public void setState(long userId, BotState state) { session(userId).state = state; }

    public void clearState(long userId) {
        UserSession s = session(userId);
        s.state = BotState.IDLE;
        s.pendingName = null;
        s.pendingYear = null;
        s.pendingMonth = null;
        s.pendingDay = null;
        s.yearPageOffset = 0;
    }

    public void setPendingName(long userId, String name) { session(userId).pendingName = name; }
    public String getPendingName(long userId) { return session(userId).pendingName; }

    public void setPendingYear(long userId, Integer year) { session(userId).pendingYear = year; }
    public Integer getPendingYear(long userId) { return session(userId).pendingYear; }

    public void setPendingMonth(long userId, Integer month) { session(userId).pendingMonth = month; }
    public Integer getPendingMonth(long userId) { return session(userId).pendingMonth; }

    public void setYearPageOffset(long userId, int offset) { session(userId).yearPageOffset = offset; }
    public int getYearPageOffset(long userId) { return session(userId).yearPageOffset; }

    public void setPendingDay(long userId, Integer day) { session(userId).pendingDay = day; }
    public Integer getPendingDay(long userId) { return session(userId).pendingDay; }

    public Lang getLanguage(long userId) {
        UserSession s = session(userId);
        if (s.lang == null) {
            s.lang = userPreferenceRepository.findById(userId)
                    .map(UserPreference::getLang)
                    .orElse(Lang.RU);
        }
        return s.lang;
    }

    public void setLanguage(long userId, Lang lang) {
        session(userId).lang = lang;
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
                .orElse("Europe/Moscow");
    }

    public void setTimezone(long userId, String timezone) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setTimezone(timezone);
        userPreferenceRepository.save(pref);
    }

    private UserPreference getOrCreatePref(long userId) {
        return userPreferenceRepository.findById(userId)
                .orElse(new UserPreference(userId, getLanguage(userId), 9, "Europe/Moscow"));
    }
}
