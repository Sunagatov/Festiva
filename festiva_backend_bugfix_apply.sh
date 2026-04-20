#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="${1:-.}"
cd "$REPO_ROOT"

if [[ ! -f "pom.xml" ]]; then
  echo "pom.xml not found. Run this from the Festiva repository root or pass the repo path as the first argument."
  exit 1
fi

if ! grep -q "<artifactId>Festiva</artifactId>" pom.xml; then
  echo "This does not look like the Festiva backend repository root."
  exit 1
fi

BACKUP_DIR="festiva_bugfix_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

backup_file() {
  local path="$1"
  mkdir -p "$BACKUP_DIR/$(dirname "$path")"
  cp "$path" "$BACKUP_DIR/$path"
}

echo "Creating backup in $BACKUP_DIR"

backup_file "src/main/java/com/festiva/friend/entity/Friend.java"
cat > "src/main/java/com/festiva/friend/entity/Friend.java" <<'EOF'
package com.festiva.friend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.util.Locale;

@Data
@Document(collection = "friends")
@NoArgsConstructor
@CompoundIndex(name = "user_normalized_name", def = "{'telegramUserId': 1, 'normalizedName': 1}", unique = true)
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    private String normalizedName;

    private Integer birthYear;
    private int birthMonth;
    private int birthDay;

    private boolean notifyEnabled = true;
    private Relationship relationship;

    public Friend(String name, LocalDate birthDate) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
    }

    public Friend(String name, LocalDate birthDate, Relationship relationship) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
        this.relationship = relationship;
    }

    public Friend(String name, Integer year, int month, int day) {
        String sanitizedName = name == null ? null : name.trim();
        if (sanitizedName == null || sanitizedName.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitizedName.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }

        this.name = sanitizedName;
        this.normalizedName = normalizeName(sanitizedName);

        try {
            if (year != null) {
                @SuppressWarnings("unused")
                LocalDate validDate = LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                MonthDay validMonthDay = MonthDay.of(month, day);
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date: " +
                    (year != null ? year + "-" : "") + month + "-" + day, e);
        }

        this.birthYear = year;
        this.birthMonth = month;
        this.birthDay = day;
    }

    public Friend(String name, Integer year, int month, int day, Relationship relationship) {
        this(name, year, month, day);
        this.relationship = relationship;
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
        this.normalizedName = normalizeName(this.name);
    }

    public boolean hasYear() {
        return birthYear != null;
    }

    public MonthDay getBirthMonthDay() {
        return MonthDay.of(birthMonth, birthDay);
    }

    public LocalDate getBirthDate() {
        if (!hasYear()) {
            throw new IllegalStateException("Birth year is unknown for " + name);
        }
        return LocalDate.of(birthYear, birthMonth, birthDay);
    }

    public int getAge(LocalDate on) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return Period.between(getBirthDate(), on).getYears();
    }

    public LocalDate nextBirthday(LocalDate from) {
        boolean isLeapDayBirthday = (birthMonth == 2 && birthDay == 29);

        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            if (hasYear()) {
                int year = from.getYear();
                while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                    year++;
                }
                next = LocalDate.of(year, 2, 29);
            } else {
                next = LocalDate.of(from.getYear(), 2, 28);
            }
        }

        if (next.isBefore(from)) {
            if (isLeapDayBirthday) {
                if (hasYear()) {
                    int year = from.getYear() + 1;
                    while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                        year++;
                    }
                    next = LocalDate.of(year, 2, 29);
                } else {
                    int year = from.getYear() + 1;
                    next = LocalDate.of(year, 1, 1).isLeapYear()
                            ? LocalDate.of(year, 2, 29)
                            : LocalDate.of(year, 2, 28);
                }
            } else {
                next = LocalDate.of(from.getYear() + 1, birthMonth, birthDay);
            }
        }

        return next;
    }

    public int getNextAge(LocalDate from) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return nextBirthday(from).getYear() - birthYear;
    }

    private static final int[][] ZODIAC_ENDS = {{1,19},{2,18},{3,20},{4,19},{5,20},{6,20},{7,22},{8,22},{9,22},{10,22},{11,21},{12,21}};
    private static final String[] ZODIAC_SIGNS = {"♑","♒","♓","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑"};

    public String getZodiac() {
        int m = birthMonth;
        int d = birthDay;
        int idx = d <= ZODIAC_ENDS[m - 1][1] ? m - 1 : m;
        return ZODIAC_SIGNS[idx];
    }
}
EOF

backup_file "src/main/java/com/festiva/friend/api/FriendService.java"
cat > "src/main/java/com/festiva/friend/api/FriendService.java" <<'EOF'
package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    public static final int FRIEND_CAP = 100;
    public static final int JUBILEE_INTERVAL = 5;
    private static final int LEAP_YEAR = 2000;

    private final FriendMongoRepository friendRepository;

    public void addFriend(long telegramUserId, Friend friend) {
        if (friend == null) {
            throw new IllegalArgumentException("Friend cannot be null");
        }

        String sanitizedName = sanitizeName(friend.getName());
        friend.setTelegramUserId(telegramUserId);
        friend.setName(sanitizedName);

        try {
            friendRepository.save(friend);
        } catch (DuplicateKeyException e) {
            log.warn("friend.create.rejected.duplicate: userId={}", telegramUserId);
            throw new IllegalArgumentException("Friend with this name already exists", e);
        }
    }

    public boolean friendExists(long telegramUserId, String name) {
        return friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, Friend.normalizeName(name));
    }

    public void deleteFriend(long telegramUserId, String name) {
        friendRepository.deleteByTelegramUserIdAndNameIgnoreCase(telegramUserId, name);
    }

    public void deleteFriendById(String id, long telegramUserId) {
        friendRepository.deleteByIdAndTelegramUserId(id, telegramUserId);
    }

    public java.util.Optional<Friend> findOwnedFriend(String id, long telegramUserId) {
        return friendRepository.findByIdAndTelegramUserId(id, telegramUserId);
    }

    public void deleteAllFriends(long telegramUserId) {
        friendRepository.deleteByTelegramUserId(telegramUserId);
    }

    public void updateFriendNameById(String id, long telegramUserId, String newName) {
        String sanitizedName = sanitizeName(newName);
        findOwnedFriend(id, telegramUserId).ifPresent(friend -> {
            String newNormalized = Friend.normalizeName(sanitizedName);
            String currentNormalized = Friend.normalizeName(friend.getName());

            if (!newNormalized.equals(currentNormalized)
                    && friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, newNormalized)) {
                throw new IllegalArgumentException("Friend with this name already exists");
            }

            friend.setName(sanitizedName);
            try {
                friendRepository.save(friend);
            } catch (DuplicateKeyException e) {
                log.warn("friend.update.rejected.duplicate: userId={}, friendId={}", telegramUserId, id);
                throw new IllegalArgumentException("Friend with this name already exists", e);
            }
        });
    }

    public void updateFriendDateById(String id, long telegramUserId, Integer year, int month, int day) {
        var existing = findOwnedFriend(id, telegramUserId);
        if (existing.isEmpty()) {
            return;
        }

        try {
            if (year != null) {
                @SuppressWarnings("unused")
                java.time.LocalDate validDate = java.time.LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                java.time.MonthDay validMonthDay = java.time.MonthDay.of(month, day);
            }
        } catch (java.time.DateTimeException e) {
            log.warn("friend.date.update.rejected.invalid: userId={}, friendId={}, hasYear={}",
                    telegramUserId, id, year != null, e);
            throw new IllegalArgumentException("Invalid date: " +
                    (year != null ? year + "-" : "") + month + "-" + day, e);
        }

        Friend friend = existing.get();
        friend.setBirthYear(year);
        friend.setBirthMonth(month);
        friend.setBirthDay(day);
        friendRepository.save(friend);
    }

    public void updateFriendRelationshipById(String id, long telegramUserId, com.festiva.friend.entity.Relationship relationship) {
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
            f.setRelationship(relationship);
            friendRepository.save(f);
        });
    }

    public boolean toggleFriendNotifyById(String id, long telegramUserId) {
        var ref = new Object() { boolean newValue = true; };
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
            f.setNotifyEnabled(!f.isNotifyEnabled());
            friendRepository.save(f);
            ref.newValue = f.isNotifyEnabled();
        });
        return ref.newValue;
    }

    public List<Friend> getFriends(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId);
    }

    public List<Friend> getFriendsSortedByDayMonth(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> java.time.LocalDate.of(LEAP_YEAR, f.getBirthMonth(), f.getBirthDay())))
                .toList();
    }

    public List<Long> getAllUserIds() {
        return friendRepository.findDistinctTelegramUserIds();
    }

    public Map<Long, List<Friend>> getFriendsByUserIds(List<Long> userIds) {
        return friendRepository.findByTelegramUserIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Friend::getTelegramUserId));
    }

    private String sanitizeName(String name) {
        String sanitized = name == null ? null : name.trim();
        if (sanitized == null || sanitized.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }
        return sanitized;
    }
}
EOF

backup_file "src/main/java/com/festiva/util/UserDateService.java"
cat > "src/main/java/com/festiva/util/UserDateService.java" <<'EOF'
package com.festiva.util;

import com.festiva.state.UserStateService;
import com.festiva.user.UserPreference;
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
EOF

backup_file "src/main/java/com/festiva/bot/DatePickerCallbackHandler.java"
cat > "src/main/java/com/festiva/bot/DatePickerCallbackHandler.java" <<'EOF'
package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class DatePickerCallbackHandler {

    static final String RELATIONSHIP_PREFIX = "RELATIONSHIP_";
    static final String EDIT_REL_PREFIX     = "EDIT_REL_";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";

    private final FriendService friendService;
    private final UserStateService userStateService;

    CallbackResult handleYearPage(String data, long userId, Lang lang) {
        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX.length()),
                "callback.date.year.page.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setYearPageOffset(userId, offset);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleYearPick(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer year = parseInteger(data.substring(DatePickerKeyboard.DATE_YEAR_PREFIX.length()),
                "callback.date.year.parse.failed", data);
        if (year == null || year <= 0) {
            return sessionExpired(lang);
        }

        userStateService.setPendingYear(userId, year);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleSkipYear(long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleMonthPick(String data, long userId, Lang lang) {
        Integer month = parseInteger(data.substring(DatePickerKeyboard.DATE_MONTH_PREFIX.length()),
                "callback.date.month.parse.failed", data);
        if (month == null || month < 1 || month > 12) {
            return sessionExpired(lang);
        }

        Integer year = userStateService.getPendingYear(userId);
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingMonth(userId, month);

        int yearForDayPicker = year != null ? year : 2000;
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_DAY, name),
                DatePickerKeyboard.dayKeyboard(yearForDayPicker, month, lang));
    }

    CallbackResult handleDayPick(String data, long userId, Lang lang) {
        Integer day = parseInteger(data.substring(DatePickerKeyboard.DATE_DAY_PREFIX.length()),
                "callback.date.day.parse.failed", data);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        String name = userStateService.getPendingName(userId);
        String id = userStateService.getPendingId(userId);

        if (day == null || month == null || name == null) {
            return sessionExpired(lang);
        }

        if (year != null) {
            try {
                LocalDate birthDate = LocalDate.of(year, month, day);
                if (birthDate.isAfter(LocalDate.now())) {
                    return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                            DatePickerKeyboard.dayKeyboard(year, month, lang));
                }
            } catch (java.time.DateTimeException e) {
                log.warn("callback.date.invalid: userId={}, date={}-{}-{}", userId, year, month, day, e);
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(year, month, lang));
            }
        } else {
            try {
                java.time.MonthDay.of(month, day);
            } catch (java.time.DateTimeException e) {
                log.warn("callback.date.invalid.monthday: userId={}, month={}, day={}", userId, month, day, e);
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(2000, month, lang));
            }
        }

        if (userStateService.getState(userId) == BotState.WAITING_FOR_EDIT_DATE) {
            if (id == null) {
                return sessionExpired(lang);
            }

            try {
                friendService.updateFriendDateById(id, userId, year, month, day);
            } catch (IllegalArgumentException e) {
                log.warn("callback.date.edit.failed: userId={}, friendId={}", userId, id, e);
                return new CallbackResult(Messages.get(lang, Messages.DATE_FUTURE_ERROR),
                        DatePickerKeyboard.dayKeyboard(year != null ? year : 2000, month, lang));
            }

            userStateService.clearState(userId);
            return new CallbackResult(Messages.get(lang, Messages.EDIT_DATE_DONE, name), null);
        }

        userStateService.setPendingYear(userId, year);
        userStateService.setPendingMonth(userId, month);
        userStateService.setPendingDay(userId, day);
        userStateService.setState(userId, BotState.WAITING_FOR_ADD_FRIEND_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, name), relationshipKeyboard(lang));
    }

    CallbackResult handleBackToYear(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        Integer offset = parseInteger(data.substring(DatePickerKeyboard.DATE_BACK_TO_YEAR.length() + 1),
                "callback.date.back.year.parse.failed", data);
        if (offset == null || offset < 0) {
            return sessionExpired(lang);
        }

        userStateService.setYearPageOffset(userId, offset);
        userStateService.setPendingYear(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_YEAR, name),
                DatePickerKeyboard.yearKeyboard(offset, lang));
    }

    CallbackResult handleBackToMonth(long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        if (name == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingMonth(userId, null);
        return new CallbackResult(Messages.get(lang, Messages.DATE_PICK_MONTH, name),
                DatePickerKeyboard.monthKeyboard(lang, userStateService.getYearPageOffset(userId)));
    }

    CallbackResult handleRelationship(String data, long userId, Lang lang) {
        String name = userStateService.getPendingName(userId);
        Integer year = userStateService.getPendingYear(userId);
        Integer month = userStateService.getPendingMonth(userId);
        Integer day = userStateService.getPendingDay(userId);
        if (month == null || day == null || name == null) {
            return sessionExpired(lang);
        }

        String value = data.substring(RELATIONSHIP_PREFIX.length());
        Relationship rel;
        if ("SKIP".equals(value)) {
            rel = null;
        } else {
            try {
                rel = Relationship.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.warn("callback.relationship.invalid: userId={}, value={}", userId, value, e);
                return sessionExpired(lang);
            }
        }

        if (friendService.getFriends(userId).size() >= FriendService.FRIEND_CAP) {
            userStateService.clearState(userId);
            return new CallbackResult(Messages.get(lang, Messages.FRIEND_CAP, FriendService.FRIEND_CAP), null);
        }

        try {
            friendService.addFriend(userId, new Friend(name, year, month, day, rel));
        } catch (IllegalArgumentException e) {
            log.warn("callback.friend.add.failed: userId={}, name={}", userId, name, e);
            userStateService.clearState(userId);
            String text = friendService.friendExists(userId, name)
                    ? Messages.get(lang, Messages.NAME_EXISTS, name)
                    : Messages.get(lang, Messages.SESSION_EXPIRED);
            return new CallbackResult(text, null);
        }

        userStateService.clearState(userId);

        String messageKey = year != null ? Messages.FRIEND_ADDED : Messages.FRIEND_ADDED_NO_YEAR;
        return new CallbackResult(Messages.get(lang, messageKey, name),
                InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_LIST)).callbackData(LIST_SORT_DATE + "_0").build(),
                        InlineKeyboardButton.builder().text(Messages.get(lang, Messages.QUICK_ADD_ANOTHER)).callbackData(CallbackQueryHandler.ACTION_ADD).build()
                ))).build());
    }

    CallbackResult handleEditFieldRel(String data, long userId, Lang lang) {
        String id = data.substring(EditCallbackHandler.EDIT_FIELD_REL.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        userStateService.setPendingName(userId, friend.getName());
        userStateService.setPendingId(userId, id);
        userStateService.setState(userId, BotState.WAITING_FOR_EDIT_RELATIONSHIP);
        return new CallbackResult(Messages.get(lang, Messages.RELATIONSHIP_PICK, friend.getName()), editRelKeyboard(lang));
    }

    CallbackResult handleEditRelationship(String data, long userId, Lang lang) {
        String id = userStateService.getPendingId(userId);
        String name = userStateService.getPendingName(userId);
        if (id == null || name == null) {
            return sessionExpired(lang);
        }

        String value = data.substring(EDIT_REL_PREFIX.length());
        Relationship rel;
        if ("SKIP".equals(value)) {
            rel = null;
        } else {
            try {
                rel = Relationship.valueOf(value);
            } catch (IllegalArgumentException e) {
                log.warn("callback.edit.relationship.invalid: userId={}, value={}", userId, value, e);
                return sessionExpired(lang);
            }
        }

        friendService.updateFriendRelationshipById(id, userId, rel);
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.EDIT_REL_DONE, name), null);
    }

    private InlineKeyboardMarkup relationshipKeyboard(Lang lang) {
        return relKeyboard(lang, RELATIONSHIP_PREFIX);
    }

    private InlineKeyboardMarkup editRelKeyboard(Lang lang) {
        return relKeyboard(lang, EDIT_REL_PREFIX);
    }

    private InlineKeyboardMarkup relKeyboard(Lang lang, String prefix) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Relationship r : Relationship.values()) {
            row.add(InlineKeyboardButton.builder().text(r.label(lang)).callbackData(prefix + r.name()).build());
            if (row.size() == 3) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.RELATIONSHIP_SKIP)).callbackData(prefix + "SKIP").build()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private Integer parseInteger(String raw, String logKey, String data) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.warn("{}: data={}", logKey, data, e);
            return null;
        }
    }

    private CallbackResult sessionExpired(Lang lang) {
        return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
    }
}
EOF

backup_file "src/main/java/com/festiva/bot/CallbackQueryHandler.java"
cat > "src/main/java/com/festiva/bot/CallbackQueryHandler.java" <<'EOF'
package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    public static final String ACTION_ADD   = "ACTION_ADD";
    public static final String ACTION_ABOUT = "ACTION_ABOUT";

    private static final String MONTH_PREFIX   = "MONTH_";
    private static final String REMOVE_PREFIX  = "REMOVE_";
    private static final String CONFIRM_PREFIX = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE  = "CANCEL_REMOVE";
    private static final String LANG_PREFIX    = "LANG_";
    private static final String CURRENT_MONTH  = "CURRENT";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";
    private static final String LIST_SORT_NAME = "LIST_SORT_NAME";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UpcomingBirthdaysCommandHandler upcomingHandler;
    private final ListCommandHandler listHandler;
    private final BulkAddCommandHandler bulkAddHandler;
    private final DatePickerCallbackHandler datePickerHandler;
    private final EditCallbackHandler editHandler;
    private final RemoveCommandHandler removeCommandHandler;
    private final EditFriendCommandHandler editFriendCommandHandler;
    private final DeleteAccountCommandHandler deleteAccountHandler;
    private final BotCommandsService commandsService;
    private final UserDateService userDateService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) {
            return null;
        }

        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) {
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        CallbackResult result = dispatch(data, chatId, userId, lang);
        if (result == null) {
            return null;
        }
        if (result.sendMessage != null) {
            return toEdit(result.sendMessage, messageId);
        }

        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode("HTML")
                .text(result.text != null ? result.text : "");
        if (result.markup != null) {
            builder.replyMarkup(result.markup);
        }
        return builder.build();
    }

    private CallbackResult dispatch(String data, long chatId, long userId, Lang lang) {
        CallbackResult r;
        if ((r = dispatchDatePicker(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchEdit(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchRemove(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchMisc(data, chatId, userId, lang)) != null) {
            return r;
        }

        log.warn("callback.unknown: userId={}, data={}", userId, data);
        return null;
    }

    private CallbackResult dispatchDatePicker(String data, long userId, Lang lang) {
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX)) {
            return datePickerHandler.handleYearPage(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX)) {
            return datePickerHandler.handleYearPick(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_SKIP_YEAR.equals(data)) {
            return datePickerHandler.handleSkipYear(userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX)) {
            return datePickerHandler.handleMonthPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX)) {
            return datePickerHandler.handleDayPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR)) {
            return datePickerHandler.handleBackToYear(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data)) {
            return datePickerHandler.handleBackToMonth(userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.RELATIONSHIP_PREFIX)) {
            return datePickerHandler.handleRelationship(data, userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.EDIT_REL_PREFIX)) {
            return datePickerHandler.handleEditRelationship(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchEdit(String data, long userId, Lang lang) {
        if (data.startsWith(EditFriendCommandHandler.EDIT_PAGE_PREFIX)) {
            return handleEditPage(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NOTIFY)) {
            return editHandler.handleEditNotify(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NAME)) {
            return editHandler.handleEditFieldName(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_DATE)) {
            return editHandler.handleEditFieldDate(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_REL)) {
            return datePickerHandler.handleEditFieldRel(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_PREFIX)) {
            return editHandler.handleEditSelect(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchRemove(String data, long userId, Lang lang) {
        if (data.startsWith(RemoveCommandHandler.REMOVE_PAGE_PREFIX)) {
            return handleRemovePage(data, userId, lang);
        }
        if (data.startsWith(CONFIRM_PREFIX)) {
            return handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        }
        if (data.startsWith(REMOVE_PREFIX)) {
            return handleRemove(data, userId, lang);
        }
        if (CANCEL_REMOVE.equals(data)) {
            return handleCancelRemove(userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchMisc(String data, long chatId, long userId, Lang lang) {
        if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) {
            return handleSettingsHour(data, userId, lang);
        }
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX)) {
            return handleSettingsTz(data, userId, lang);
        }
        if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) {
            return handleUpcoming(data, userId, lang);
        }
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX)) {
            return handleListPage(data, userId, lang);
        }
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) {
            return handleListSort(data, userId, lang);
        }
        if (data.startsWith(LANG_PREFIX)) {
            return handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        }
        if (data.startsWith(MONTH_PREFIX)) {
            return handleMonth(userId, data, lang);
        }

        switch (data) {
            case ACTION_ADD -> {
                return handleActionAdd(userId, lang);
            }
            case ACTION_ABOUT -> {
                return new CallbackResult(Messages.get(lang, Messages.ABOUT), null);
            }
            case BulkAddCommandHandler.CALLBACK_PASTE -> {
                return new CallbackResult(bulkAddHandler.promptPaste(chatId, userId, lang));
            }
            case BulkAddCommandHandler.CALLBACK_CSV -> {
                bulkAddHandler.sendCsvTemplate(chatId, lang);
                return null;
            }
            case BulkAddCommandHandler.CALLBACK_ICS -> {
                userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
                return new CallbackResult(Messages.get(lang, Messages.ICS_PROMPT), null);
            }
            case DeleteAccountCommandHandler.CONFIRM_DELETE -> {
                return handleConfirmDeleteAccount(userId, lang);
            }
            case DeleteAccountCommandHandler.CANCEL_DELETE -> {
                return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_CANCEL), null);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CONFIRM -> {
                return handleIcsConfirm(userId, lang);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CANCEL -> {
                userStateService.clearState(userId);
                return new CallbackResult(Messages.get(lang, Messages.ICS_CANCELLED), null);
            }
            default -> {
                return null;
            }
        }
    }

    private CallbackResult handleSettingsHour(String data, long userId, Lang lang) {
        try {
            int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
            if (hour < 0 || hour > 23) {
                log.warn("callback.settings.hour.invalid: userId={}, hour={}", userId, hour);
                return sessionExpired(lang);
            }
            userStateService.setNotifyHour(userId, hour);
            return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                    SettingsCommandHandler.combined(hour, userStateService.getTimezone(userId)));
        } catch (NumberFormatException e) {
            log.warn("callback.settings.hour.parse.failed: data={}", data, e);
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleSettingsTz(String data, long userId, Lang lang) {
        String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
        try {
            @SuppressWarnings("unused")
            java.time.ZoneId validatedZone = java.time.ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.warn("callback.settings.tz.invalid: userId={}, tz={}", userId, tz, e);
            return sessionExpired(lang);
        }
        userStateService.setTimezone(userId, tz);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId), tz));
    }

    private CallbackResult handleListSort(String data, long userId, Lang lang) {
        boolean byDate = data.startsWith(LIST_SORT_DATE);
        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private CallbackResult handleListPage(String data, long userId, Lang lang) {
        String suffix = data.substring(ListCommandHandler.LIST_PAGE_PREFIX.length());
        boolean byDate;
        if (suffix.startsWith("DATE_")) {
            byDate = true;
        } else if (suffix.startsWith("NAME_")) {
            byDate = false;
        } else {
            log.warn("callback.list.page.invalid.mode: data={}", data);
            return sessionExpired(lang);
        }

        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private Integer parsePageSuffix(String data) {
        int idx = data.lastIndexOf('_');
        if (idx < 0) {
            log.warn("callback.page.parse.failed: missing suffix, data={}", data);
            return null;
        }

        try {
            int page = Integer.parseInt(data.substring(idx + 1));
            if (page < 0) {
                log.warn("callback.page.parse.failed: negative page, data={}", data);
                return null;
            }
            return page;
        } catch (NumberFormatException e) {
            log.warn("callback.page.parse.failed: data={}", data, e);
            return null;
        }
    }

    private CallbackResult handleUpcoming(String data, long userId, Lang lang) {
        try {
            int days = Integer.parseInt(data.substring(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX.length()));
            if (!Set.of(7, 14, 30).contains(days)) {
                log.warn("callback.upcoming.days.invalid: userId={}, days={}", userId, days);
                return sessionExpired(lang);
            }

            var friends = friendService.getFriends(userId);
            return new CallbackResult(upcomingHandler.buildText(friends, lang, days, userId),
                    upcomingHandler.filterKeyboard(lang, days));
        } catch (NumberFormatException e) {
            log.warn("callback.upcoming.days.parse.failed: data={}", data, e);
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleActionAdd(long userId, Lang lang) {
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_ADD_FRIEND_NAME);
        return new CallbackResult(Messages.get(lang, Messages.ENTER_NAME), null);
    }

    private CallbackResult handleLanguage(long userId, String code) {
        try {
            Lang newLang = Lang.valueOf(code);
            userStateService.setLanguage(userId, newLang);

            commandsService.updateCommandsForUser(userId, newLang);

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(new InlineKeyboardRow(
                            InlineKeyboardButton.builder().text((newLang == Lang.EN ? "✅ " : "") + Messages.get(newLang, Messages.LANG_EN_BTN)).callbackData(LANG_PREFIX + Lang.EN.name()).build(),
                            InlineKeyboardButton.builder().text((newLang == Lang.RU ? "✅ " : "") + Messages.get(newLang, Messages.LANG_RU_BTN)).callbackData(LANG_PREFIX + Lang.RU.name()).build()
                    )))
                    .build();
            return new CallbackResult(Messages.get(newLang, Messages.LANGUAGE_SET), keyboard);
        } catch (IllegalArgumentException e) {
            log.warn("callback.language.unknown: code={}", code, e);
            return sessionExpired(userStateService.getLanguage(userId));
        }
    }

    private CallbackResult handleRemovePage(String data, long userId, Lang lang) {
        String rawPage = data.substring(RemoveCommandHandler.REMOVE_PAGE_PREFIX.length());
        try {
            int page = Integer.parseInt(rawPage);
            if (page < 0) {
                return sessionExpired(lang);
            }

            var friends = friendService.getFriendsSortedByDayMonth(userId);
            if (friends.isEmpty()) {
                return new CallbackResult(Messages.get(lang, Messages.FRIENDS_EMPTY), null);
            }
            return new CallbackResult(Messages.get(lang, Messages.SELECT_REMOVE),
                    removeCommandHandler.keyboard(friends, page));
        } catch (NumberFormatException e) {
            log.warn("callback.remove.page.parse.failed: data={}", data, e);
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleEditPage(String data, long userId, Lang lang) {
        String rawPage = data.substring(EditFriendCommandHandler.EDIT_PAGE_PREFIX.length());
        try {
            int page = Integer.parseInt(rawPage);
            if (page < 0) {
                return sessionExpired(lang);
            }

            var friends = friendService.getFriendsSortedByDayMonth(userId);
            if (friends.isEmpty()) {
                return new CallbackResult(Messages.get(lang, Messages.FRIENDS_EMPTY), null);
            }
            return new CallbackResult(Messages.get(lang, Messages.EDIT_SELECT),
                    editFriendCommandHandler.keyboard(friends, page));
        } catch (NumberFormatException e) {
            log.warn("callback.edit.page.parse.failed: data={}", data, e);
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleRemove(String data, long userId, Lang lang) {
        String id = data.substring(REMOVE_PREFIX.length());
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        String name = friend.getName();
        userStateService.setPendingName(userId, name);
        userStateService.setPendingId(userId, id);
        userStateService.setState(userId, com.festiva.state.BotState.WAITING_FOR_REMOVE_CONFIRM);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_ASK, name), confirmKeyboard(id, lang));
    }

    private CallbackResult handleConfirmRemove(long userId, String id, Lang lang) {
        Friend friend = friendService.findOwnedFriend(id, userId).orElse(null);
        if (friend == null) {
            return sessionExpired(lang);
        }

        String name = friend.getName();
        friendService.deleteFriendById(id, userId);
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.FRIEND_REMOVED, name), null);
    }

    private CallbackResult handleCancelRemove(long userId, Lang lang) {
        userStateService.clearState(userId);
        return new CallbackResult(Messages.get(lang, Messages.CONFIRM_REMOVE_CANCEL), null);
    }

    private CallbackResult handleConfirmDeleteAccount(long userId, Lang lang) {
        deleteAccountHandler.deleteAccount(userId);
        return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_DONE), null);
    }

    private CallbackResult handleIcsConfirm(long userId, Lang lang) {
        List<com.festiva.friend.entity.Friend> pending = userStateService.getPendingIcsImport(userId);
        if (pending == null || pending.isEmpty()) {
            userStateService.clearState(userId);
            return sessionExpired(lang);
        }

        List<Friend> currentFriends = friendService.getFriends(userId);
        Set<String> existingNames = currentFriends.stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(java.util.stream.Collectors.toSet());

        int currentCount = currentFriends.size();
        int saved = 0;

        for (com.festiva.friend.entity.Friend friend : pending) {
            String normalizedName = Friend.normalizeName(friend.getName());
            if (normalizedName.isBlank() || existingNames.contains(normalizedName)) {
                continue;
            }

            if (currentCount + saved >= FriendService.FRIEND_CAP) {
                log.warn("ics.import.cap.reached: userId={}, cap={}", userId, FriendService.FRIEND_CAP);
                break;
            }

            try {
                friendService.addFriend(userId, friend);
                existingNames.add(normalizedName);
                saved++;
            } catch (IllegalArgumentException e) {
                log.warn("ics.import.save.rejected: userId={}, name={}", userId, friend.getName(), e);
            } catch (Exception e) {
                log.warn("ics.import.save.failed: userId={}", userId, e);
            }
        }

        userStateService.clearState(userId);

        String message = saved > 0
                ? Messages.get(lang, Messages.ICS_DONE, saved)
                : Messages.get(lang, Messages.ICS_NONE_SAVED);
        return new CallbackResult(message, null);
    }

    private CallbackResult handleMonth(long userId, String data, Lang lang) {
        String value = data.substring(MONTH_PREFIX.length());
        int month;
        if (CURRENT_MONTH.equalsIgnoreCase(value)) {
            month = userDateService.todayFor(userId).getMonthValue();
        } else {
            try {
                month = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("callback.month.parse.failed: data={}", data, e);
                return new CallbackResult(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
            }
        }

        if (month < 1 || month > 12) {
            log.warn("callback.month.invalid: data={}, month={}", data, month);
            return new CallbackResult(Messages.get(lang, Messages.MONTH_PARSE_ERROR), null);
        }

        var filtered = friendService.getFriendsSortedByDayMonth(userId).stream()
                .filter(f -> f.getBirthMonthDay().getMonthValue() == month)
                .toList();

        String raw = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, lang.locale());
        String monthName = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        if (filtered.isEmpty()) {
            return new CallbackResult(Messages.get(lang, Messages.BIRTHDAYS_NONE, monthName), null);
        }

        LocalDate today = userDateService.todayFor(userId);
        StringBuilder sb = new StringBuilder(Messages.get(lang, Messages.BIRTHDAYS_HEADER, monthName));
        filtered.forEach(f -> {
            String dateStr = f.hasYear()
                    ? f.getBirthDate().format(MessageBuilder.DATE_FORMATTER)
                    : String.format("%02d.%02d", f.getBirthMonthDay().getDayOfMonth(), f.getBirthMonthDay().getMonthValue());

            sb.append("– <b>").append(dateStr)
                    .append("</b> ").append(com.festiva.util.HtmlEscaper.escape(f.getName()));

            if (f.hasYear()) {
                boolean alreadyCelebrated = f.nextBirthday(today).getYear() > today.getYear();
                String ageLabel = alreadyCelebrated
                        ? Messages.get(lang, Messages.YEARS_OLD, Messages.yearsRu(lang, f.getAge(today)))
                        : Messages.get(lang, Messages.YEARS_TURNS, Messages.yearsRu(lang, f.getNextAge(today)));
                sb.append(" (<i>").append(ageLabel).append("</i>)");
            }

            sb.append("\n");
        });
        return new CallbackResult(sb.toString(), null);
    }

    private InlineKeyboardMarkup confirmKeyboard(String id, Lang lang) {
        return InlineKeyboardMarkup.builder().keyboard(List.of(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_YES)).callbackData(CONFIRM_PREFIX + id).build(),
                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.CONFIRM_NO)).callbackData(CANCEL_REMOVE).build()
        ))).build();
    }

    private EditMessageText toEdit(SendMessage msg, int messageId) {
        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(msg.getChatId())
                .messageId(messageId)
                .parseMode("HTML")
                .text(msg.getText());

        if (msg.getReplyMarkup() instanceof InlineKeyboardMarkup inlineKeyboardMarkup) {
            builder.replyMarkup(inlineKeyboardMarkup);
        }

        return builder.build();
    }

    private CallbackResult sessionExpired(Lang lang) {
        return new CallbackResult(Messages.get(lang, Messages.SESSION_EXPIRED), null);
    }
}
EOF

backup_file "src/main/java/com/festiva/command/handler/BulkAddCommandHandler.java"
cat > "src/main/java/com/festiva/command/handler/BulkAddCommandHandler.java" <<'EOF'
package com.festiva.command.handler;

import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkAddCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_PASTE = "BULK_PASTE";
    public static final String CALLBACK_CSV   = "BULK_CSV";
    public static final String CALLBACK_ICS   = "BULK_ICS";

    private static final int FILE_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int FILE_READ_TIMEOUT_MILLIS = 10_000;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() {
        return "/addmany";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_BULK_ADD);
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_PASTE_BTN)).callbackData(CALLBACK_PASTE).build(),
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_CSV_BTN)).callbackData(CALLBACK_CSV).build()
                        ),
                        new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text(Messages.get(lang, Messages.BULK_ADD_ICS_BTN)).callbackData(CALLBACK_ICS).build()
                        )
                )).build();

        return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_CHOOSE), keyboard);
    }

    public void sendCsvTemplate(long chatId, Lang lang) {
        try {
            String csv = "name,birthday,relationship\nAlice,15.03.1990,friend\nBob,22.07.1985,\n";
            InputFile file = new InputFile(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    "friends_template.csv");
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .document(file)
                    .caption(Messages.get(lang, Messages.BULK_ADD_CSV_CAPTION))
                    .build());
        } catch (TelegramApiException e) {
            log.warn("bulk.add.template.send.failed: chatId={}", chatId, e);
        }
    }

    public SendMessage promptPaste(long chatId, long userId, Lang lang) {
        userStateService.setState(userId, BotState.WAITING_FOR_BULK_ADD);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        List<String> lines;

        if (update.getMessage().hasDocument()) {
            lines = downloadDocument(update);
            if (lines == null) {
                return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_FILE_INVALID));
            }
        } else if (update.getMessage().hasText()) {
            lines = Arrays.asList(update.getMessage().getText().split("\n"));
        } else {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.BULK_ADD_FILE_INVALID));
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(Collectors.toSet());

        BulkAddParser.ParseResult result = BulkAddParser.parse(lines, existing, lang);

        if (result.noData()) {
            return MessageBuilder.html(chatId, result.errors().getFirst());
        }

        int currentCount = existing.size();
        List<Friend> toAdd = result.valid();
        List<String> errors = new ArrayList<>(result.errors());

        if (currentCount + toAdd.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            if (allowed < toAdd.size()) {
                errors.add(Messages.get(lang, Messages.BULK_CAP_EXCEEDED, allowed, FriendService.FRIEND_CAP));
                toAdd = toAdd.subList(0, allowed);
            }
        }

        List<Friend> added = new ArrayList<>();
        for (Friend friend : toAdd) {
            try {
                friendService.addFriend(userId, friend);
                added.add(friend);
            } catch (IllegalArgumentException e) {
                log.warn("bulk.add.row.rejected: userId={}, name={}", userId, friend.getName(), e);
                errors.add(Messages.get(lang, Messages.NAME_EXISTS, friend.getName()));
            }
        }

        userStateService.clearState(userId);

        return MessageBuilder.html(chatId, buildResponse(lang, new BulkAddParser.ParseResult(added, errors, false)));
    }

    private String buildResponse(Lang lang, BulkAddParser.ParseResult result) {
        StringBuilder sb = new StringBuilder();
        if (!result.valid().isEmpty()) {
            sb.append(Messages.get(lang, Messages.BULK_ADD_SUCCESS, result.valid().size()));
        }
        if (!result.errors().isEmpty()) {
            String errorList = result.errors().stream()
                    .map(e -> "• " + e)
                    .collect(Collectors.joining("\n"));
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(Messages.get(lang, Messages.BULK_ADD_ERRORS, result.errors().size(), errorList));
        }
        if (sb.isEmpty()) {
            sb.append(Messages.get(lang, Messages.BULK_ADD_EMPTY));
        }
        return sb.toString();
    }

    private List<String> downloadDocument(Update update) {
        try {
            var doc = update.getMessage().getDocument();
            String mime = doc.getMimeType();
            if (mime != null && !mime.startsWith("text/") && !mime.equals("application/octet-stream")) {
                return null;
            }
            if (doc.getFileSize() != null && doc.getFileSize() > 512_000) {
                return null;
            }

            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(doc.getFileId()).build());

            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(FILE_CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(FILE_READ_TIMEOUT_MILLIS);
            connection.setUseCaches(false);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (TelegramApiException | IOException e) {
            log.warn("bulk.add.file.download.failed", e);
            return null;
        }
    }
}
EOF

backup_file "src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java"
cat > "src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java" <<'EOF'
package com.festiva.command.handler;

import com.festiva.ai.IcsNameExtractorService;
import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportIcsCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_ICS_CONFIRM = "ICS_CONFIRM";
    public static final String CALLBACK_ICS_CANCEL  = "ICS_CANCEL";

    private static final DateTimeFormatter ICS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private static final int FILE_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int FILE_READ_TIMEOUT_MILLIS = 10_000;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Autowired(required = false)
    private IcsNameExtractorService icsNameExtractorService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() {
        return "/importics";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_ICS_FILE);
    }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);

        if (!update.getMessage().hasDocument()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NOT_A_FILE));
        }

        var doc = update.getMessage().getDocument();
        String mime = doc.getMimeType();
        if (mime != null && !mime.startsWith("text/") && !mime.equals("application/octet-stream")) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_WRONG_TYPE));
        }
        if (doc.getFileSize() != null && doc.getFileSize() > 15_000_000) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_TOO_LARGE));
        }

        List<String> lines = downloadLines(doc.getFileId());
        if (lines == null) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PARSE_ERROR));
        }

        List<IcsEntry> entries = extractYearlyEntries(lines);
        if (entries.isEmpty()) {
            userStateService.clearState(userId);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NO_EVENTS));
        }

        List<Friend> candidates = new ArrayList<>();
        for (IcsEntry entry : entries) {
            try {
                candidates.add(convertToFriend(entry));
            } catch (IllegalArgumentException e) {
                log.warn("ics.import.entry.skipped.invalid: summary={}", entry.summary(), e);
            }
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(Collectors.toSet());

        List<Friend> toSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new java.util.HashSet<>();

        for (Friend friend : candidates) {
            String normalized = Friend.normalizeName(friend.getName());
            if (existing.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_EXISTS, 0, friend.getName()));
            } else if (seenInBatch.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DUPLICATE, 0, friend.getName()));
            } else if (friend.getName().length() > 100) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_NAME_LONG, 0));
            } else {
                seenInBatch.add(normalized);
                toSave.add(friend);
            }
        }

        int currentCount = existing.size();
        if (currentCount + toSave.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            if (allowed < toSave.size()) {
                errors.add(Messages.get(lang, Messages.BULK_CAP_EXCEEDED, allowed, FriendService.FRIEND_CAP));
                toSave = toSave.subList(0, allowed);
            }
        }

        if (toSave.isEmpty()) {
            userStateService.clearState(userId);
            String preview = buildPreviewLines(List.of(), errors);
            return MessageBuilder.html(chatId,
                    Messages.get(lang, Messages.ICS_PREVIEW_NO_VALID, entries.size(), preview));
        }

        userStateService.setPendingIcsImport(userId, toSave);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_CONFIRM);

        String preview = buildPreviewLines(toSave, errors);
        String text = Messages.get(lang, Messages.ICS_PREVIEW, entries.size(), preview, toSave.size());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CONFIRM_BTN, toSave.size()))
                                .callbackData(CALLBACK_ICS_CONFIRM).build(),
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CANCEL_BTN))
                                .callbackData(CALLBACK_ICS_CANCEL).build()
                ))).build();
        return MessageBuilder.html(chatId, text, kb);
    }

    public record IcsEntry(String summary, LocalDate date, boolean yearTrusted) {}

    public static List<IcsEntry> extractYearlyEntries(List<String> raw) {
        List<String> unfolded = unfold(raw);
        List<IcsEntry> result = new ArrayList<>();
        String summary = null;
        String dtstart = null;
        boolean inEvent = false;
        boolean yearly = false;
        boolean isBirthday = false;

        for (String line : unfolded) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true;
                summary = null;
                dtstart = null;
                yearly = false;
                isBirthday = false;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && dtstart != null) {
                    boolean accept = yearly || isBirthday ||
                            (summary != null && summary.toLowerCase(Locale.ROOT).matches(".*(birthday|bday|born).*"));

                    if (accept && summary != null) {
                        LocalDate date = parseIcsDate(dtstart);
                        if (date != null) {
                            boolean yearTrusted = date.isBefore(LocalDate.now()) &&
                                    date.getYear() > 1900 &&
                                    date.getYear() < LocalDate.now().getYear();
                            result.add(new IcsEntry(summary.trim(), date, yearTrusted));
                        }
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                if (line.startsWith("SUMMARY:")) {
                    summary = line.substring(8);
                } else if (line.startsWith("DTSTART")) {
                    int colon = line.indexOf(':');
                    if (colon >= 0) {
                        dtstart = line.substring(colon + 1).trim();
                    }
                } else if (line.startsWith("RRULE:") && line.contains("FREQ=YEARLY")) {
                    yearly = true;
                } else if (line.startsWith("X-GOOGLE-CALENDAR-CONTENT-TYPE:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("CATEGORIES:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("BDAY:")) {
                    dtstart = line.substring(5).trim();
                    isBirthday = true;
                }
            }
        }
        return result;
    }

    public static List<String> extractYearlyCsvLines(List<String> raw) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
        return extractYearlyEntries(raw).stream()
                .map(entry -> entry.summary() + "," + entry.date().format(fmt))
                .toList();
    }

    private String resolveName(String summary) {
        if (icsNameExtractorService == null) {
            return summary == null ? null : summary.trim();
        }

        try {
            String extracted = icsNameExtractorService.extractName(summary);
            if (extracted == null || extracted.isBlank()) {
                return summary == null ? null : summary.trim();
            }
            return extracted.trim();
        } catch (Exception e) {
            log.warn("ics.ai.name.extraction.failed", e);
            return summary == null ? null : summary.trim();
        }
    }

    private Friend convertToFriend(IcsEntry entry) {
        String name = resolveName(entry.summary());
        LocalDate date = entry.date();

        if (!entry.yearTrusted()) {
            return new Friend(name, null, date.getMonthValue(), date.getDayOfMonth());
        }

        return new Friend(name, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static List<String> unfold(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                if (!out.isEmpty()) {
                    out.set(out.size() - 1, out.getLast() + line.substring(1));
                }
            } else {
                out.add(line.replace("\r", ""));
            }
        }
        return out;
    }

    private static LocalDate parseIcsDate(String value) {
        String datePart = value.length() >= 8 ? value.substring(0, 8) : value;
        try {
            return LocalDate.parse(datePart, ICS_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPreviewLines(List<Friend> valid, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (Friend friend : valid) {
            String dateStr = friend.hasYear()
                    ? friend.getBirthDate().format(CSV_DATE_FMT)
                    : String.format("%02d.%02d.", friend.getBirthMonthDay().getDayOfMonth(), friend.getBirthMonthDay().getMonthValue());
            sb.append("✅ ").append(friend.getName())
                    .append(" — ").append(dateStr).append("\n");
        }
        for (String error : errors) {
            sb.append("❌ ").append(error).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private List<String> downloadLines(String fileId) {
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(fileId).build());

            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(FILE_CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(FILE_READ_TIMEOUT_MILLIS);
            connection.setUseCaches(false);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (TelegramApiException | IOException e) {
            log.warn("ics.import.file.download.failed", e);
            return null;
        }
    }
}
EOF

echo "Bugfix files written successfully."
echo "Backup saved under: $BACKUP_DIR"
echo
echo "Next steps:"
echo "  1) Review the diff: git diff --stat && git diff"
echo "  2) Run verification: bash festiva_backend_bugfix_verify.sh"
