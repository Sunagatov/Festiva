# Feature: Jubilee (Milestone Birthdays)

> **Status:** `Stable`
> **Command:** `/jubilee`
> **Handler:** `JubileeCommandHandler.java`

---

## 1. Overview

Shows all friends whose next birthday is a milestone age — a multiple of 5 (e.g. 25, 30, 35, 40).
Results are sorted by next birthday date ascending.

---

## 2. User Stories

- As a user, I want to see upcoming milestone birthdays so that I can plan something special for round-number ages.

---

## 3. Functional Requirements

1. `/jubilee` lists all friends whose next age (`getNextAge(today)`) is a positive multiple of `JUBILEE_INTERVAL` (5).
2. Friends are sorted by next birthday date ascending.
3. Each entry shows: next birthday date, friend name, age they will turn, and days until (or 🎂 if today).
4. If no friends have upcoming jubilees, bot shows an empty state with a `/upcomingbirthdays` hint.
5. If the user has no friends at all, bot shows `friends_empty`.
6. Friends turning age 0 are excluded (`nextAge > 0` guard).

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`. Age label uses `Messages.yearsRu()` for correct Russian grammatical forms.
- **Jubilee interval:** Hardcoded as `FriendService.JUBILEE_INTERVAL = 5`.
- **Date basis:** Uses server `LocalDate.now()` — not the user's configured timezone.

---

## 5. Bot Flow

### Happy Path

```
User sends: /jubilee
  → Bot fetches all friends, sorts by next birthday
  → Filters to jubilee friends only
  → Bot: "Milestone birthdays
          – 15.03 Alice (turns 30) (in 45d)
          – 22.07 Bob (turns 25) 🎂" [message key: jubilee_header]
```

### No Jubilees

```
User has friends but none have a milestone birthday coming up
  → Bot: "🏆 No upcoming milestone birthdays.
          Use /upcomingbirthdays to see all upcoming birthdays." [message key: jubilee_none]
```

### No Friends

```
User has no friends
  → Bot: "👥 No friends yet. Use /add to add your first one." [message key: friends_empty]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| No friends | Empty friend list | `friends_empty` shown |
| Friends exist but no jubilees | No friend has next age % 5 == 0 | `jubilee_none` shown |
| Jubilee birthday is today | `daysUntil = 0` | 🎂 shown instead of day count |
| Friend turning age 0 | Born today, `nextAge = 0` | Excluded from results |
| Multiple jubilees same day | Tie in next birthday date | Order follows stream sort (stable by `nextBirthday`) |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No friends | `friends_empty` | "👥 **No friends yet.** Use /add to add your first one." |
| No jubilees | `jubilee_none` | "🏆 **No upcoming milestone birthdays.** Use /upcomingbirthdays to see all upcoming birthdays." |

---

## 9. Acceptance Criteria

- [ ] Given a friend whose next age is 30 (multiple of 5), then they appear in the jubilee list.
- [ ] Given a friend whose next age is 31 (not a multiple of 5), then they do not appear.
- [ ] Given no friends have jubilee birthdays, then `jubilee_none` is shown with `/upcomingbirthdays` hint.
- [ ] Given the user has no friends, then `friends_empty` is shown.
- [ ] Given a jubilee birthday is today, then 🎂 is shown instead of a day count.
- [ ] Given multiple jubilee friends, then results are sorted by next birthday date ascending.
- [ ] Given the user's language is RU, then all messages are in Russian with correct age grammar.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `birthDate` | Used to compute `nextBirthday` and `nextAge` |
| `name` | Displayed in results |
| `telegramUserId` | Scopes the query to the current user |

---

## 11. Security & Privacy

- **Ownership:** Friends fetched by `telegramUserId` — users only see their own data.
- **Deletion:** No data written; nothing to delete.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

No structured log events for this feature.

---

## 13. Known Limitations

- **Uses server date** — `LocalDate.now()` is not timezone-aware. A user in UTC+12 near midnight may see jubilees calculated against yesterday's date.
- **Jubilee interval (5) is hardcoded** in `FriendService.JUBILEE_INTERVAL` — not configurable.
- **No time window** — all future jubilees are shown regardless of how far away they are. A friend turning 25 in 11 months appears alongside one turning 30 tomorrow.

---

## 14. Relationships to Other Features

- Jubilee logic shared with: `stats` (jubilee count)
- Related to: `upcoming-birthdays`, `today`
- Data source: `add-friend`, `bulk-add`
- Affected by: `remove-friend`, `delete-account`

---

## 15. Out of Scope

- Configurable jubilee interval
- Time-window filtering (e.g. jubilees in the next 30 days only)
- Custom milestone ages (e.g. 18, 21)

---

## 16. Open Questions

- [ ] Should jubilees be filtered to a time window (e.g. next 365 days) to avoid showing distant future milestones?
- [ ] Should the jubilee interval be configurable per user?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `JubileeCommandHandlerTest.java` | Jubilee friend shown, non-jubilee excluded, no friends → `friends_empty`, no jubilees → hint contains `/upcomingbirthdays`, RU language |

**Not covered by tests:**
- Jubilee birthday today → 🎂 shown
- Multiple jubilee friends sorted by date
- `nextAge = 0` guard (friend born today excluded)
- Server timezone vs user timezone mismatch

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
