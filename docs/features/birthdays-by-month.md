# Feature: Birthdays by Month

> **Status:** `Stable`
> **Command:** `/birthdays`
> **Handlers:** `BirthdaysCommandHandler.java`, `CallbackQueryHandler.java` (`handleMonth`)

---

## 1. Overview

Allows a user to browse their friends' birthdays month by month.
The user selects a month from an inline keyboard and sees all friends with birthdays in that month, along with their age information.

---

## 2. User Stories

- As a user, I want to browse birthdays by month so that I can plan ahead for a specific month.
- As a user, I want to see how many friends have birthdays in each month so that I can quickly spot busy months.
- As a user, I want to jump directly to the current month so that I don't have to scroll.

---

## 3. Functional Requirements

1. `/birthdays` shows a month picker keyboard with all 12 months.
2. Each month button shows the friend count for that month in parentheses if count > 0 (e.g. `Jan (3)`).
3. The current month button is prefixed with 📍.
4. A "Current month" shortcut button is always shown at the top of the keyboard.
5. Months are displayed in 4 columns per row (3 rows of 4 months).
6. When a month is selected, bot shows all friends with birthdays in that month, sorted by day.
7. Each entry shows: date, name, and age label (turned X this year / turns X this year).
8. If no friends have birthdays in the selected month, bot shows an empty state message with a suggestion to try another month or use `/add`.

---

## 4. Non-Functional Requirements

- **i18n:** Month names are localised using `Month.getDisplayName(TextStyle.SHORT, lang.locale())`. All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Month name formatting:** First letter capitalised, trailing dot removed (e.g. Russian `янв.` → `Янв`).
- **Age label logic:** If the friend's birthday has already passed this calendar year → "turned X this year". If not yet → "turns X this year".

---

## 5. Bot Flow

### Happy Path

```
User sends: /birthdays
  → Bot fetches friend list and counts per month
  → Bot: "View birthdays — Select a month:" + month picker keyboard [message key: birthdays_pick]

User taps: "Current month" button
  → Bot resolves current month from system date
  → Bot: "🎂 Birthdays — March\n\n– 15.03 Alice (turns 35 this year)\n– 22.03 Bob (turned 40 this year)" [message key: birthdays_header]

User taps: any month button (e.g. "Jan (2)")
  → Bot: "🎂 Birthdays — January\n\n– 05.01 Carol (...)\n– 19.01 Dave (...)"
```

### Empty Month

```
User taps a month with no friends
  → Bot: "📅 No birthdays in April. Try another month or use /add to add a friend." [message key: birthdays_none]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| No friends at all | User has empty friend list | All month buttons show no count; selecting any month shows `birthdays_none` |
| Current month button tapped | `MONTH_CURRENT` callback | Resolved to current month value from `LocalDate.now()` |
| Invalid month value in callback | Malformed callback data | `month_parse_error` |
| Friend born on Feb 29 | Leap day birthday | Displayed on Feb 29 in leap years; Feb 28 in non-leap years |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.
The entire interaction is driven by inline keyboard callbacks.

> States are defined in `BotState.java`

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No birthdays in selected month | `birthdays_none` | "📅 No birthdays in **%s**. Try another month or use /add to add a friend." |
| Invalid month callback | `month_parse_error` | "⚠️ Couldn't select that month. Please try again." |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends in January and March, when `/birthdays` is sent, then January and March buttons show counts and other months do not.
- [ ] Given the current month is July, when `/birthdays` is sent, then the July button is prefixed with 📍.
- [ ] Given the user taps "Current month", then bot shows birthdays for the current calendar month.
- [ ] Given the user taps a month with friends, then bot shows all friends sorted by day with correct age labels.
- [ ] Given the user taps a month with no friends, then bot shows `birthdays_none` with the month name.
- [ ] Given a friend's birthday has already passed this year, then age label says "turned X this year".
- [ ] Given a friend's birthday has not yet occurred this year, then age label says "turns X this year".
- [ ] Given the user's language is RU, then month names and all messages are in Russian.

---

## 10. Data Model

No data is written by this feature. Reads from the `friends` collection.

| Field Read | Source | Notes |
|---|---|---|
| `birthDate` | `Friend` | Used to group by month and compute age |
| `name` | `Friend` | Displayed in the birthday list |
| `telegramUserId` | `Friend` | Scopes the query to the current user |

---

## 11. Security & Privacy

- **Ownership:** Friend list is fetched by `telegramUserId` — users can only see their own friends.
- **Deletion:** No data written; nothing to delete.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

No structured log events for this feature.
Malformed month callback data is logged at `WARN` via `callback.month.parse.failed` in `CallbackQueryHandler`.

| Event | Log Level | Key Fields |
|---|---|---|
| `callback.month.parse.failed` | `WARN` | `data` |

---

## 13. Known Limitations

- The month picker keyboard is re-rendered with fresh counts only when `/birthdays` is sent. Tapping a month and going back does not refresh the counts — the original keyboard remains.
- Feb 29 birthdays are shown on Feb 28 in non-leap years (handled by `Friend.nextBirthday()`), but the month view filters strictly by `birthDate.getMonthValue()` — so a Feb 29 friend always appears in February regardless of year.

---

## 14. Relationships to Other Features

- Depends on: `add-friend`, `bulk-add` (data source)
- Related to: `today`, `upcoming-birthdays` (alternative birthday browsing views)

---

## 15. Out of Scope

- Navigating between months with prev/next arrows (no pagination — user returns to picker)
- Showing birthdays across multiple months at once
- Filtering by relationship type

---

## 16. Open Questions

- [ ] Should tapping a month update the keyboard in-place (edit message) or send a new message? Currently it edits in-place via `EditMessageText`.
- [ ] Should the month picker refresh counts when re-opened after adding a friend?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `BirthdaysCommandHandlerTest.java` | Prompt text, keyboard row count, friend count shown on month button, RU language |

**Not covered by tests:**
- Month selection callback (`handleMonth` in `CallbackQueryHandler`)
- Empty month response (`birthdays_none`)
- Age label logic (turned vs turns)
- Current month 📍 marker
- `month_parse_error` on malformed callback
- Feb 29 edge case

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
