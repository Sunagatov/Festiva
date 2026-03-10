# Feature: Today's Birthdays

> **Status:** `Stable`
> **Command:** `/today`
> **Handler:** `TodayCommandHandler.java`

---

## 1. Overview

Shows all friends whose birthday falls on today's date.
Each entry shows the friend's name and the age they are turning.

---

## 2. User Stories

- As a user, I want to see whose birthday is today so that I can congratulate them immediately.

---

## 3. Functional Requirements

1. `/today` lists all friends whose `nextBirthday(today)` equals today.
2. Each entry shows: 🎂, friend name, and age they are turning.
3. If no friends have a birthday today, bot shows an empty state with hints to `/upcomingbirthdays` and `/add`.
4. Results are in the order returned by `getFriends()` — no explicit sort applied.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`. Age label uses `Messages.yearsRu()` for correct Russian grammatical forms.
- **Date basis:** Uses server `LocalDate.now()` — not the user's configured timezone.

---

## 5. Bot Flow

### Happy Path

```
User sends: /today
  → Bot fetches all friends
  → Filters to friends where nextBirthday(today) == today
  → Bot: "Today's birthdays:
          🎂 Alice — turns 30
          🎂 Bob — turns 25
          Use /list to see all friends or /upcomingbirthdays for what's coming next." [message keys: today_header, today_hint]
```

### No Birthdays Today

```
User sends: /today with no matches
  → Bot: "🎂 No birthdays today. Check /upcomingbirthdays to see what's coming up, or /add to add a friend." [message key: today_none]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| No friends | Empty friend list | `today_none` shown |
| No birthdays today | Friends exist but none match today | `today_none` shown |
| Feb 29 birthday in non-leap year | `nextBirthday()` advances to next leap year | Friend does NOT appear on Feb 28 or Feb 29 in non-leap years |
| Multiple birthdays today | Several friends share today's date | All shown, in `getFriends()` order |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No birthdays today | `today_none` | "🎂 **No birthdays today.** Check /upcomingbirthdays to see what's coming up, or /add to add a friend." |

---

## 9. Acceptance Criteria

- [ ] Given a friend's birthday is today, then their name and age appear in the response.
- [ ] Given no friends have a birthday today, then `today_none` is shown.
- [ ] Given the user has no friends, then `today_none` is shown.
- [ ] Given multiple friends share today's birthday, then all are shown.
- [ ] Given the user's language is RU, then all messages are in Russian with correct age grammar.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `birthDate` | Used to compute `nextBirthday(today)` |
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

- **Uses server date** — not the user's configured timezone. A user in UTC+12 at midnight may miss a birthday that is "today" in their timezone but "yesterday" on the server.
- **No sort** — results are in DB insertion order, not alphabetical or by age.
- **Feb 29 birthdays** — in non-leap years, `nextBirthday()` skips to the next leap year entirely. The friend will not appear on Feb 28 via `/today` even though the scheduler sends a reminder on Feb 28.

---

## 14. Relationships to Other Features

- Related to: `upcoming-birthdays`, `jubilee`, `birthdays-by-month`
- Data source: `add-friend`, `bulk-add`
- Affected by: `remove-friend`, `delete-account`

---

## 15. Out of Scope

- Showing yesterday's or tomorrow's birthdays
- Timezone-aware "today" calculation

---

## 16. Open Questions

- [ ] Should `/today` use the user's configured timezone instead of server date, consistent with how the scheduler works?
- [ ] Should Feb 29 birthdays appear on Feb 28 in non-leap years (matching scheduler behaviour)?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `TodayCommandHandlerTest.java` | No birthdays today → `today_none`, birthday today → name shown, next-step hint, RU language |

**Not covered by tests:**
- Multiple friends with birthday today
- Feb 29 birthday in non-leap year
- Age label correctness
- Server timezone vs user timezone mismatch

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
