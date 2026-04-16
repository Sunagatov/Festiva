package com.festiva.state;

import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserStateService {

    private final ConcurrentHashMap<Long, UserSession> cache = new ConcurrentHashMap<>();
    private final UserSessionRepository sessionRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    private UserSession session(long userId) {
        return cache.computeIfAbsent(userId, id -> {
            UserSession session = sessionRepository.findById(id).orElse(new UserSession());
            session.setUserId(id);
            session.touch();
            return session;
        });
    }
    
    private void saveSession(long userId) {
        UserSession session = cache.get(userId);
        if (session != null) {
            session.touch();
            sessionRepository.save(session);
        }
    }

    public BotState getState(long userId) { return session(userId).getState(); }
    public void setState(long userId, BotState state) { 
        session(userId).setState(state);
        saveSession(userId);
    }

    public void clearState(long userId) {
        UserSession s = session(userId);
        s.setState(BotState.IDLE);
        s.setPendingName(null);
        s.setPendingId(null);
        s.setPendingYear(null);
        s.setPendingMonth(null);
        s.setPendingDay(null);
        s.setYearPageOffset(0);
        s.setPendingIcsImport(null);
        saveSession(userId);
    }

    public void removeSession(long userId) {
        cache.remove(userId);
        sessionRepository.deleteById(userId);
    }

    public void setPendingName(long userId, String name) { 
        session(userId).setPendingName(name);
        saveSession(userId);
    }
    public String getPendingName(long userId) { return session(userId).getPendingName(); }

    public void setPendingId(long userId, String id) { 
        session(userId).setPendingId(id);
        saveSession(userId);
    }
    public String getPendingId(long userId) { return session(userId).getPendingId(); }

    public void setPendingYear(long userId, Integer year) { 
        session(userId).setPendingYear(year);
        saveSession(userId);
    }
    public Integer getPendingYear(long userId) { return session(userId).getPendingYear(); }

    public void setPendingMonth(long userId, Integer month) { 
        session(userId).setPendingMonth(month);
        saveSession(userId);
    }
    public Integer getPendingMonth(long userId) { return session(userId).getPendingMonth(); }

    public void setYearPageOffset(long userId, int offset) { 
        session(userId).setYearPageOffset(offset);
        saveSession(userId);
    }
    public int getYearPageOffset(long userId) { return session(userId).getYearPageOffset(); }

    public void setPendingDay(long userId, Integer day) { 
        session(userId).setPendingDay(day);
        saveSession(userId);
    }
    public Integer getPendingDay(long userId) { return session(userId).getPendingDay(); }

    public void setPendingIcsImport(long userId, java.util.List<Friend> friends) { 
        session(userId).setPendingIcsImport(friends);
        saveSession(userId);
    }
    public java.util.List<Friend> getPendingIcsImport(long userId) { return session(userId).getPendingIcsImport(); }

    public Lang getLanguage(long userId) {
        UserSession s = session(userId);
        if (s.getLang() == null) {
            Lang lang = userPreferenceRepository.findById(userId)
                    .map(UserPreference::getLang)
                    .orElse(UserPreference.DEFAULT_LANG);
            s.setLang(lang);
        }
        return s.getLang();
    }

    public void setLanguage(long userId, Lang lang) {
        session(userId).setLang(lang);
        saveSession(userId);
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

    private UserPreference getOrCreatePref(long userId) {
        return userPreferenceRepository.findById(userId)
                .orElse(new UserPreference(userId, UserPreference.DEFAULT_LANG, 9, UserPreference.DEFAULT_TIMEZONE, null));
    }
}
