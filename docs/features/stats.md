# Feature: Stats

> **Status:** `Stable`
> **Command:** `/stats`
> **Handler:** `StatsCommandHandler.java`

---

## 1. Overview

Shows a summary of the user's birthday data: total friends, next upcoming birthday, friends with birthdays this calendar month, and upcoming milestone (jubilee) birthdays.

---

## 2. User Stories

- As a user, I want to see a quick summary of my birthday data so that I can get an overview without browsing individual lists.

---

## 3. Functional Requirements

1. `/stats` shows a single message with four stats:
   - **Total friends** — count of all friends.
   - **Next birthday** — name of the friend with the soonest upcoming birthday, with days remaining (or 🎂 if today).
   - **This month** — count of friends with birthdays in the current calendar month.
   - **Upcoming jubilees** — count of friends whose next age is a multiple of `JUBILEE_INTERVAL` (5).
2. If the user has no friends, next birthday shows `—`.
3. This feature is stateless and read-only.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Jubilee interval:** Hardcoded as `FriendService.JUBILEE_INTERVAL = 5`.
- **"This month"** is based on the current calendar month of the server's `LocalDate.now()`, not the user's timezone.

---

## 5. Bot Flow

```
User sends: /stats
  → Bot fetches all friends
  → Bot: "📊 Your Festiva stats
          👥 Friends: 12
          🎂 Next birthday: Alice (in 3d)
          📅 This month: 2
          🏆 Upcoming jubilees: 1" [message key: stats_header]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| No friends | Empty friend list | All counts are 0; next birthday shows `—` |
| Birthday today | `daysUntil = 0` | Next birthday shows `Name 🎂` |
| Multiple friends same soonest day | Tie in `daysUntil` | First in stream order wins (not deterministic) |
| Friend turning age 0 | `nextAge = 0` (born today) | Excluded from jubilee count (`nextAge > 0` guard) |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.

---

## 8. Error Messages

No error messages — this feature is read-only with no user input.

| Message Key | EN Text |
|---|---|
| `stats_header` | "📊 **Your Festiva stats** 👥 Friends: **%d** 🎂 Next birthday: %s 📅 This month: **%d** 🏆 Upcoming jubilees: **%d**" |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends, when `/stats` is sent, then total friend count is shown correctly.
- [ ] Given the user has friends, then the friend with the soonest next birthday is shown as "Next birthday".
- [ ] Given a friend's birthday is today, then next birthday shows `Name 🎂`.
- [ ] Given the user has no friends, then next birthday shows `—` and all counts are 0.
- [ ] Given friends with birthdays in the current month, then "This month" count is correct.
- [ ] Given a friend whose next age is a multiple of 5, then jubilee count includes them.
- [ ] Given a friend whose next age is not a multiple of 5, then jubilee count excludes them.
- [ ] Given the user's language is RU, then the message is in Russian.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `birthDate` | Used for all four stat calculations |
| `name` | Shown in "Next birthday" field |
| `telegramUserId` | Scopes the query to the current user |

---

## 11. Security & Privacy

- **Ownership:** Friends fetched by `telegramUserId` — users only see their own stats.
- **Deletion:** No data written; nothing to delete.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

No structured log events for this feature.

---

## 13. Known Limitations

- **"This month" uses server date** — not the user's configured timezone. A user in UTC+12 at midnight could see "this month" calculated against yesterday's server date.
- **Tie-breaking for "Next birthday" is non-deterministic** — if two friends share the same soonest birthday, the one returned depends on stream order (insertion order from DB).
- **Jubilee count includes friends whose birthday has already passed this year** — `getNextAge(today)` returns the age they will turn on their *next* birthday, which could be next year. This is correct behaviour but worth being explicit about.

---

## 14. Relationships to Other Features

- Data source: `add-friend`, `bulk-add`, `edit-friend`
- Jubilee logic shared with: `jubilee` feature
- Affected by: `remove-friend`, `delete-account`

---

## 15. Out of Scope

- Historical stats (e.g. how many birthdays were celebrated last year)
- Per-relationship-type breakdown
- Stats refresh without re-running `/stats`

---

## 16. Open Questions

- [ ] Should "This month" use the user's configured timezone instead of server date?
- [ ] Should tie-breaking for "Next birthday" be deterministic (e.g. alphabetical by name)?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `StatsCommandHandlerTest.java` | Friend count shown, next birthday name shown, no friends → dash, birthday today → 🎂, RU language |

**Not covered by tests:**
- "This month" count correctness
- Jubilee count correctness
- Tie-breaking for next birthday
- `nextAge = 0` guard (friend born today excluded from jubilee)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
