# Feature: Settings

> **Status:** `Stable`
> **Command:** `/settings`
> **Handler:** `SettingsCommandHandler.java`, `CallbackQueryHandler.java` (`handleSettingsHour`, `handleSettingsTz`)

---

## 1. Overview

Allows a user to configure when and in which timezone they receive birthday reminders.
Settings are persisted in `user_preferences` and consumed by the `birthday-reminders` scheduler.

---

## 2. User Stories

- As a user, I want to choose what time of day I receive reminders so that I'm not notified at inconvenient hours.
- As a user, I want to set my timezone so that reminders arrive at the correct local time.

---

## 3. Functional Requirements

1. `/settings` shows a combined keyboard: 24-hour picker (top) + timezone picker (bottom).
2. The currently active hour and timezone are marked with ✅ in the keyboard.
3. Tapping an hour saves it immediately and refreshes the keyboard in-place.
4. Tapping a timezone saves it immediately and refreshes the keyboard in-place.
5. Available timezones are a fixed predefined list of 12 options (not free-text entry).
6. Hour buttons cover all 24 hours (00:00–23:00), displayed in 4 columns.
7. Timezone buttons are displayed in 3 columns.
8. After saving, bot shows a confirmation message with the new value.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Persistence:** Settings saved to `user_preferences` collection via `UserStateService`.
- **Immediate effect:** Changes take effect on the next scheduler run — no restart required.
- **Fixed timezone list:** Timezones are hardcoded in `SettingsCommandHandler.TIMEZONES`. Users cannot enter arbitrary timezone strings.

---

## 5. Bot Flow

### Happy Path

```
User sends: /settings
  → Bot reads current notifyHour and timezone from UserStateService
  → Bot: "⏰ Notification time — Choose when to receive daily reminders:
          🌍 Timezone — Choose your timezone:"
        + combined hour + timezone keyboard [message keys: settings_header, settings_tz_header]

User taps: "09:00" (hour button)
  → Hour saved to user_preferences
  → Keyboard refreshed in-place with ✅ on 09:00
  → Bot: "✅ Reminders set to 09:00." [message key: settings_hour_set]

User taps: "Moscow" (timezone button)
  → Timezone saved to user_preferences
  → Keyboard refreshed in-place with ✅ on Moscow
  → Bot: "✅ Timezone set to Europe/Moscow." [message key: settings_tz_set]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| User taps already-active hour | Taps ✅ 09:00 again | Saved again (no-op effectively); confirmation shown |
| User taps already-active timezone | Taps ✅ Moscow again | Saved again (no-op effectively); confirmation shown |
| User has no saved preferences | First time opening settings | Defaults shown: hour=9, timezone=`Europe/Moscow` |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.
All interaction is via inline keyboard callbacks.

---

## 8. Error Messages

No error messages — all inputs are constrained to valid button values.

| Scenario | Message Key | EN Text |
|---|---|---|
| Hour saved | `settings_hour_set` | "✅ Reminders set to **%02d:00**. Use /settings to change other preferences." |
| Timezone saved | `settings_tz_set` | "✅ Timezone set to **%s**. Use /settings to change other preferences." |

---

## 9. Acceptance Criteria

- [ ] Given the user opens `/settings`, then bot shows a keyboard with all 24 hours and all 12 timezones.
- [ ] Given the user's current hour is 9, then the `09:00` button is marked with ✅.
- [ ] Given the user's current timezone is `Europe/Moscow`, then the Moscow button is marked with ✅.
- [ ] Given the user taps an hour button, then the hour is saved and confirmation `settings_hour_set` is shown.
- [ ] Given the user taps a timezone button, then the timezone is saved and confirmation `settings_tz_set` is shown.
- [ ] Given the user has no saved preferences, then defaults (hour=9, timezone=`Europe/Moscow`) are shown.
- [ ] Given the user's language is RU, then all messages are in Russian.

---

## 10. Data Model

Reads and writes `user_preferences` collection via `UserStateService`.

| Field | Type | Notes |
|---|---|---|
| `notifyHour` | `int` | 0–23; `-1` means unset → defaults to `9` in scheduler |
| `timezone` | `String` | Must be a valid `ZoneId` string from the predefined list |

---

## 11. Security & Privacy

- **Ownership:** Settings are scoped to `telegramUserId` — users can only change their own preferences.
- **Deletion:** `user_preferences` document is removed on `/deleteaccount`.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

No structured log events for this feature.
Hour and timezone changes are saved silently via `UserStateService`.

---

## 13. Known Limitations

- **Fixed timezone list** — only 12 timezones available. Users in unlisted timezones (e.g. `Asia/Kolkata`, `Australia/Sydney`) cannot set their correct timezone.
- **No free-text timezone entry** — by design (prevents invalid `ZoneId` values), but limits coverage.
- **Hour granularity is 1 hour** — users cannot set reminders at e.g. 09:30.
- **`lastNotifiedDate` is not reset when settings change** — if a user changes their notify hour after already being notified today, they will not receive a second notification that day even if the new hour has not yet passed.

---

## 14. Relationships to Other Features

- Directly configures: `birthday-reminders` (notify hour + timezone)
- Preferences stored alongside: `language` (same `user_preferences` document)
- Cleared by: `delete-account`

---

## 15. Out of Scope

- Free-text timezone entry
- Sub-hour notification granularity (e.g. 09:30)
- Per-friend notification time overrides
- Notification frequency settings (e.g. disable 7-day reminder)

---

## 16. Open Questions

- [ ] Should the timezone list be expanded or made configurable via environment variable?
- [ ] Should changing the notify hour reset `lastNotifiedDate` so the user can receive a notification at the new hour on the same day?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `SettingsCommandHandlerTest.java` | Settings header shown, all 24 hours in keyboard, active hour ✅ marker, active timezone ✅ marker, RU language |

**Not covered by tests:**
- Hour save callback (`handleSettingsHour`) — value persisted, keyboard refreshed
- Timezone save callback (`handleSettingsTz`) — value persisted, keyboard refreshed
- Default values shown when no preferences exist
- `lastNotifiedDate` not reset on hour change

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
