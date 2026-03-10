# Feature: Upcoming Birthdays

> **Status:** `Stable`
> **Command:** `/upcomingbirthdays`
> **Handler:** `UpcomingBirthdaysCommandHandler.java`

---

## 1. Overview

Shows all friends with birthdays within a configurable day window (7, 14, or 30 days).
Includes today's birthdays. Results are sorted by next birthday date ascending.
The user can switch the window via inline buttons without re-running the command.

---

## 2. User Stories

- As a user, I want to see upcoming birthdays in the next 30 days so that I can plan ahead.
- As a user, I want to narrow the window to 7 or 14 days so that I can focus on what's imminent.

---

## 3. Functional Requirements

1. `/upcomingbirthdays` shows friends with birthdays within the next **30 days** by default (inclusive of today).
2. User can switch the window to **7** or **14** days via inline buttons; the list updates in-place.
3. Available windows: 7, 14, 30 days — hardcoded.
4. The active window is marked with ✅ on the keyboard.
5. Each entry shows: date (DD.MM), friend name, age they will turn, and days remaining.
6. Today's birthdays show a special label instead of a day count.
7. Results are sorted by next birthday date ascending.
8. If no friends fall within the window, bot shows an empty state with a hint to try a wider window or use `/add`.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`. Age label uses `Messages.yearsRu()` for correct Russian grammatical forms.
- **Date basis:** Uses server `LocalDate.now()` — not the user's configured timezone.
- **Window filter:** `daysUntil >= 0 && daysUntil <= daysLimit` — includes today (0) and the exact boundary day.

---

## 5. Bot Flow

### Happy Path

```
User sends: /upcomingbirthdays
  → Default window: 30 days
  → Bot: "Upcoming birthdays:
          – 15.03 Alice (turns 30, days left — 3)
          – 22.03 Bob 🎂 TODAY! turns 25"
        + [7d] [14d] [✅ 30d] keyboard [message key: upcoming_header]

User taps: "7d"
  → List filtered to 7-day window, updated in-place
  → Keyboard: [✅ 7d] [14d] [30d]
```

### Empty Window

```
User taps "7d" with no friends in that window
  → Bot: "📅 No birthdays in the next 7 days. Try a wider window above, or use /add to add a friend." [message key: upcoming_none]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| No friends | Empty friend list | `upcoming_none` shown for default 30-day window |
| Birthday today | `daysUntil = 0` | Special `upcoming_today` label shown |
| Birthday exactly on boundary | `daysUntil == daysLimit` | Included in results |
| Narrower window has no results | e.g. 7d window empty | `upcoming_none` with hint to try wider window |
| Feb 29 in non-leap year | `nextBirthday()` advances to next leap year | Friend excluded from current year's window |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.
Window switching is handled via inline keyboard callbacks.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No upcoming birthdays | `upcoming_none` | "📅 **No birthdays in the next %d days.** Try a wider window above, or use /add to add a friend." |

---

## 9. Acceptance Criteria

- [ ] Given a friend's birthday is in 5 days, when `/upcomingbirthdays` is sent, then they appear in the default 30-day list.
- [ ] Given a friend's birthday is in 31 days, then they do not appear in the 30-day list.
- [ ] Given a friend's birthday is today, then the `upcoming_today` label is shown.
- [ ] Given the user taps "7d", then only friends within 7 days are shown and the 7d button has ✅.
- [ ] Given no friends fall within the selected window, then `upcoming_none` is shown with the day count.
- [ ] Given multiple friends in the window, then results are sorted by next birthday date ascending.
- [ ] Given the user has no friends, then `upcoming_none` is shown.
- [ ] Given the user's language is RU, then all messages are in Russian with correct age grammar.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `birthDate` | Used to compute `nextBirthday` and `daysUntil` |
| `name` | Displayed in results |
| `telegramUserId` | Scopes the query to the current user |

---

## 11. Security & Privacy

- **Ownership:** Friends fetched by `telegramUserId` — users only see their own data.
- **Deletion:** No data written; nothing to delete.

---

## 12. Metrics & Observability

No structured log events for this feature.

---

## 13. Known Limitations

- **Uses server date** — not the user's configured timezone. Same issue as `today`, `stats`, `jubilee`.
- **Fixed window options** — only 7, 14, 30 days available. Hardcoded in `filterKeyboard()`.
- **Feb 29 birthdays** — in non-leap years, `nextBirthday()` advances to the next leap year, so the friend is excluded from all windows in non-leap years.
- **No pagination** — all results returned in one message. With 100 friends and a 30-day window, the message could approach Telegram's 4096-character limit.

---

## 14. Relationships to Other Features

- Related to: `today`, `jubilee`, `birthdays-by-month`
- Linked from: `jubilee` empty state hint
- Data source: `add-friend`, `bulk-add`
- Affected by: `remove-friend`, `delete-account`

---

## 15. Out of Scope

- Custom day window (free-text entry)
- Timezone-aware "today" calculation
- Paginated results

---

## 16. Open Questions

- [ ] Should the day window options be configurable or expanded (e.g. 60, 90 days)?
- [ ] Should `/upcomingbirthdays` use the user's configured timezone for date calculation?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `UpcomingBirthdaysCommandHandlerTest.java` | Friend within 30 days shown, friend outside 30 days excluded, no friends → `upcoming_none`, RU language, filter keyboard has 3 buttons |

**Not covered by tests:**
- Birthday today → `upcoming_today` label
- Window switching callback (7d, 14d)
- ✅ marker on active window button
- Boundary day included (exactly 30 days away)
- Multiple friends sorted by date
- Feb 29 in non-leap year

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
