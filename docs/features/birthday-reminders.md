# Feature: Birthday Reminders

> **Status:** `Stable`
> **Command:** none (scheduled background job)
> **Handlers:** `BirthdayReminder.java`, `NotificationSender.java`

---

## 1. Overview

A scheduled background job that runs every hour and sends Telegram messages to users whose friends have birthdays today, tomorrow, or in exactly 7 days — but only at the user's configured notification hour in their configured timezone.
Each user is notified at most once per calendar day.

---

## 2. User Stories

- As a user, I want to receive a reminder on a friend's birthday so that I don't forget to congratulate them.
- As a user, I want to receive a reminder one day before so that I have time to prepare.
- As a user, I want to receive a reminder one week before so that I can plan ahead.
- As a user, I want reminders at my preferred time of day in my timezone so that I'm not woken up at night.
- As a user, I want to mute reminders for specific friends so that I have control over what I receive.

---

## 3. Functional Requirements

1. The scheduler runs every hour at minute 0 (`0 0 * * * *` UTC cron).
2. Reminders are sent for birthdays exactly **0**, **1**, or **7** days away — no other intervals.
3. Each user has a configurable **notify hour** (default: `9`) and **timezone** (default: `Europe/Moscow`).
4. A user is only processed if the current hour in their timezone matches their notify hour.
5. A user is only notified once per calendar day — `lastNotifiedDate` is checked and updated after processing.
6. Per-friend `notifyEnabled` flag is respected — friends with `notifyEnabled=false` are silently skipped.
7. Notification message includes: friend name, relationship label (if set), zodiac sign, age turning, and a deep link to the bot.
8. If sending a notification fails for one friend, the error is logged and processing continues for other friends.
9. On application startup, `checkBirthdaysOnStartup()` runs once immediately (catches up if the bot was down during the scheduled hour).
10. If a user's timezone is invalid, that user is skipped silently.
11. Default language is **Russian** if no language preference is set.

---

## 4. Non-Functional Requirements

- **Schedule:** Runs every hour on the hour, UTC (`@Scheduled(cron = "0 0 * * * *", zone = "UTC")`).
- **Timezone handling:** Each user's local date is derived from UTC time + their stored timezone.
- **Deduplication:** `lastNotifiedDate` (stored in `user_preferences`) prevents duplicate notifications within the same calendar day.
- **Fault isolation:** A `RuntimeException` from `NotificationSender.send()` is caught per-friend — one failure does not block other users or friends.
- **Startup check:** Runs once at `@PostConstruct` to handle bot restarts during the notification window. Failures are caught and logged as `WARN`.
- **MDC logging:** `userId` is added to MDC context during per-user processing for log correlation.
- **i18n:** Notification messages use the user's stored language. Falls back to `Lang.RU` if not set.

---

## 5. Reminder Flow

### Per-hour execution

```
Scheduler fires at HH:00 UTC
  → Fetch all userIds with at least one friend
  → Fetch all UserPreferences for those userIds
  → Fetch all friends grouped by userId

For each userId:
  → Resolve user's timezone (default: UTC if missing)
  → Convert UTC now → user's local time
  → Check if user's local hour == notifyHour (default: 9)
  → Check if lastNotifiedDate != today (dedup guard)
  → For each friend:
      → Skip if notifyEnabled = false
      → Compute daysUntil = days between today and nextBirthday
      → If daysUntil ∈ {0, 1, 7}: send notification
  → Update lastNotifiedDate = today
```

### Notification message content

```
Today:    "🎂 Today is <name> <relationship>'s birthday <zodiac> — turning <age>! 👉 Open Festiva"
Tomorrow: "🔔 Tomorrow is <name> <relationship>'s birthday <zodiac> — turning <age>! 👉 Open Festiva"
In 7d:    "📅 In one week it's <name> <relationship>'s birthday <zodiac> — turning <age>! 👉 Open Festiva"
```

---

## 6. Edge Cases

| Scenario | Behaviour |
|---|---|
| User has no friends | Fetched but no notifications sent; `lastNotifiedDate` still updated |
| Friend's `notifyEnabled = false` | Skipped silently |
| Birthday in 2, 3, 4, 5, 6 days | No notification sent |
| User already notified today | Skipped entirely (dedup guard) |
| User's notify hour doesn't match current UTC hour in their timezone | Skipped |
| Invalid timezone in preferences | User skipped, `WARN` logged |
| `NotificationSender.send()` throws | Error logged per friend, processing continues |
| Bot restarted during notification window | Startup check fires immediately and catches up |
| No `UserPreference` record for a user | Defaults applied: hour=9, timezone=UTC, lang=RU |
| Feb 29 birthday in non-leap year | `Friend.nextBirthday()` advances to next leap year — reminder fires on Feb 28 in non-leap years |

---

## 7. State Transitions

This feature has no `BotState` changes. It is a background job with no user interaction.

---

## 8. Error Messages

Reminders are outbound messages, not responses to user input. No error messages are shown to users.

| Event | Behaviour |
|---|---|
| Send failure | `reminder.notify.failed` logged at `ERROR`; user sees nothing |
| Invalid timezone | `reminder.timezone.invalid` logged at `WARN`; user sees nothing |
| Startup check failure | `reminder.startup.check.failed` logged at `WARN` |

---

## 9. Acceptance Criteria

- [ ] Given a friend's birthday is today and notify hour matches, then a `notify_today` message is sent.
- [ ] Given a friend's birthday is tomorrow and notify hour matches, then a `notify_tomorrow` message is sent.
- [ ] Given a friend's birthday is in 7 days and notify hour matches, then a `notify_week` message is sent.
- [ ] Given a friend's birthday is in 10 days, then no notification is sent.
- [ ] Given the current UTC hour does not match the user's notify hour in their timezone, then no notification is sent.
- [ ] Given `lastNotifiedDate` equals today, then no notification is sent (dedup).
- [ ] Given `notifyEnabled = false` on a friend, then no notification is sent for that friend.
- [ ] Given an invalid timezone in preferences, then the user is skipped and no exception is thrown.
- [ ] Given `NotificationSender.send()` throws, then the exception is caught and other friends are still processed.
- [ ] Given the user has no language preference, then the notification is sent in Russian.
- [ ] Given the notification is sent, then it contains the friend's name and age.

---

## 10. Data Model

Reads from `friends`. Reads and writes `user_preferences`.

**`user_preferences` collection:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `telegramUserId` | `long` | ✅ | Document `_id` |
| `lang` | `Lang` | ❌ | Defaults to `RU` if null |
| `notifyHour` | `int` | ✅ | `-1` means unset → defaults to `9` |
| `timezone` | `String` | ❌ | Defaults to `Europe/Moscow`; must be a valid `ZoneId` |
| `lastNotifiedDate` | `LocalDate` | ❌ | Set after each successful processing run; used for dedup |

**`friends` collection (read only):**

| Field Read | Notes |
|---|---|
| `telegramUserId` | Groups friends by user |
| `birthDate` | Used to compute `nextBirthday` and `daysUntil` |
| `name` | Included in notification message |
| `relationship` | Included in notification message if set |
| `notifyEnabled` | Skips friend if `false` |

---

## 11. Security & Privacy

- **Ownership:** Friends are fetched grouped by `telegramUserId` — no cross-user data access.
- **Exposure:** `telegramUserId` is used internally only; never included in notification message text.
- **Bot token:** Used in the deep link URL (`t.me/<botUsername>`) — username only, not the token itself.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `reminder.check.start` | `INFO` | `utcHour` |
| `reminder.check.done` | `INFO` | `userCount`, `notifiedCount` |
| `reminder.notify.sent` | `DEBUG` | `userId`, `friend`, `daysUntil` |
| `reminder.notify.failed` | `ERROR` | `userId`, `friend`, `message` |
| `reminder.timezone.invalid` | `WARN` | `tz` |
| `reminder.startup.check.failed` | `WARN` | — |
| MDC context | — | `userId` set during per-user processing |

---

## 13. Known Limitations

- **Startup check uses current UTC time** — if the bot restarts at 10:05 UTC and a user's notify hour is 9 in UTC, the startup check will not re-send (hour mismatch). The dedup guard also prevents re-sending even if the hour matched.
- **No retry mechanism** — if `NotificationSender.send()` fails, the notification is lost. There is no queue or retry.
- **`lastNotifiedDate` is updated even if no friends had birthdays** — any user with friends gets their `lastNotifiedDate` stamped, which prevents a second attempt later the same day even if the first run had zero notifications to send.
- **Feb 29 birthdays** — in non-leap years, `nextBirthday()` advances to the next leap year. The reminder fires on the correct future date, but the "days until" calculation may skip Feb 28 of the current year entirely.
- **Single timezone per user** — all friends of a user share the user's timezone. There is no per-friend timezone.

---

## 14. Relationships to Other Features

- Depends on: `add-friend`, `bulk-add` (data source)
- Configured by: `settings` (notify hour, timezone)
- Per-friend mute controlled by: `edit-friend` (notify toggle)
- Language controlled by: `language`

---

## 15. Out of Scope

- Custom reminder intervals (e.g. 3 days, 14 days)
- Per-friend custom reminder intervals
- Retry on send failure
- Push notifications outside Telegram

---

## 16. Open Questions

- [ ] Should `lastNotifiedDate` only be stamped when at least one notification was actually sent, rather than on every processing run?
- [ ] Should the startup check be skipped if `lastNotifiedDate` already equals today (to avoid double-send on fast restarts)?
- [ ] Should send failures trigger a retry (e.g. exponential backoff)?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `BirthdayReminderTest.java` (integration) | Today/tomorrow/7-day notifications, irrelevant date, send failure isolation, wrong hour, notify disabled, dedup guard, RU language, age in message, invalid timezone |

**Not covered by tests:**
- Startup check (`@PostConstruct`) behaviour
- `lastNotifiedDate` stamped even when no notifications sent
- Feb 29 birthday edge case
- User with no `UserPreference` record (full defaults path)
- Multiple friends for same user — partial failure (one fails, others succeed)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
