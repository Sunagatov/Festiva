# Feature: List Friends

> **Status:** `Stable`
> **Command:** `/list`
> **Handler:** `ListCommandHandler.java`, `CallbackQueryHandler.java` (`handleListSort`, `handleListPage`)

---

## 1. Overview

Shows the user's full friends list with birthday details, age labels, zodiac signs, and relationship labels.
Supports two sort modes (by date, by name) and pagination (10 per page).
Sort and page can be changed in-place via inline buttons.

---

## 2. User Stories

- As a user, I want to see all my friends and their birthdays so that I have a full overview.
- As a user, I want to sort by date so that I can see who's coming up next in the calendar year.
- As a user, I want to sort by name so that I can find a specific friend quickly.

---

## 3. Functional Requirements

1. `/list` shows friends sorted by day/month (by-date mode) by default, page 0.
2. If the friend list is empty, bot shows `friends_empty` with an "➕ Add a friend" button.
3. Each entry shows: birthdate, zodiac sign, name, relationship label (if set), age label, and days until next birthday (or 🎂 if today).
4. **By-date mode** splits the page into two sections: "Coming up" (birthday not yet passed this year) and "Already celebrated" (birthday already passed this year).
5. **By-name mode** shows all friends on the page in alphabetical order with no section split.
6. Pagination: 10 friends per page. ◀ / ▶ navigation shown when needed.
7. Sort mode and page are preserved across navigation — switching sort resets to page 0.
8. Age label:
   - Birthday already passed this year → "turned X this year"
   - Birthday not yet this year → "currently X, turns Y this year"

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`. Age labels use `Messages.yearsRu()` for correct Russian grammatical forms.
- **Sort by name:** Case-insensitive, locale-root alphabetical order.
- **Date basis:** Uses server `LocalDate.now()` — not the user's configured timezone.
- **Pagination:** 10 per page (`ListCommandHandler.PAGE_SIZE`). Page resets to 0 if out of bounds.

---

## 5. Bot Flow

### Happy Path — By Date

```
User sends: /list
  → Default: by-date, page 0
  → Bot: "Friends (current calendar year):
          Coming up:
          – 15.03 ♈ Alice 👫 Friend (currently 29, turns 30 this year) (in 3d)
          – 22.07 ♋ Bob (currently 24, turns 25 this year) (in 132d)
          Already celebrated:
          – 05.01 ♑ Carol (turned 40 this year) 🎂"
        + [✅ 📅 By date] [🔤 By name] keyboard
```

### Sort Switch

```
User taps: "🔤 By name"
  → List re-rendered alphabetically, page 0
  → Keyboard: [📅 By date] [✅ 🔤 By name]
```

### Pagination

```
User taps: "▶" (next page)
  → Next 10 friends shown, same sort mode
  → Page indicator: "Page 2 / 4"
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| No friends | Empty list | `friends_empty` + Add button |
| Birthday today | `daysUntil = 0` | 🎂 shown instead of day count; appears in "Coming up" section |
| All birthdays passed this year | All friends in "Already celebrated" | "Coming up" section omitted |
| All birthdays still ahead | All friends in "Coming up" | "Already celebrated" section omitted |
| Page out of bounds | Stale callback with old page | Resets to page 0 |
| Single page | ≤ 10 friends | No pagination buttons shown; no page indicator |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.
All interaction is via inline keyboard callbacks.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No friends | `friends_empty` | "👥 **No friends yet.** Use /add to add your first one." |
| Turned label | `list_turned` | "(turned **%s** this year)" |
| Will turn label | `list_will_turn` | "(currently **%s**, turns **%s** this year)" |
| Today | `list_days_today` | 🎂 |
| Days left | `list_days_left` | "(in %dd)" |
| Page indicator | `list_page` | "Page %d / %d" |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends, when `/list` is sent, then friends are shown sorted by day/month.
- [ ] Given the user has no friends, then `friends_empty` is shown with an Add button.
- [ ] Given a friend's birthday has passed this year, then they appear in "Already celebrated" with "turned X" label.
- [ ] Given a friend's birthday is still ahead this year, then they appear in "Coming up" with "turns X" label.
- [ ] Given a friend has a relationship set, then the relationship label appears in their entry.
- [ ] Given a friend's birthday is today, then 🎂 is shown.
- [ ] Given the user taps "By name", then friends are shown in alphabetical order with no section split.
- [ ] Given more than 10 friends, then pagination buttons are shown and page indicator is shown.
- [ ] Given the user's language is RU, then all messages are in Russian with correct age grammar.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `birthDate` | Sorting, age calculation, section split |
| `name` | Displayed; used for alphabetical sort |
| `relationship` | Displayed as localised label if set |
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

- **Uses server date** — not the user's configured timezone. Same systemic issue as `today`, `stats`, `jubilee`, `upcoming-birthdays`, `birthdays-by-month`.
- **"Coming up" / "Already celebrated" split is per-page** — if page 1 has only "Already celebrated" friends and page 2 has only "Coming up" friends, the sections appear on separate pages, which can be confusing.
- **Pagination page size (10) is hardcoded** in `ListCommandHandler.PAGE_SIZE`.
- **Feb 29 birthdays** — in non-leap years, `nextBirthday()` advances to the next leap year. The friend appears in "Already celebrated" for the entire non-leap year.

---

## 14. Relationships to Other Features

- Data source: `add-friend`, `bulk-add`, `edit-friend`
- Affected by: `remove-friend`, `delete-account`
- Empty state links to: `add-friend`
- Related to: `search`, `birthdays-by-month`, `upcoming-birthdays`

---

## 15. Out of Scope

- Filtering by relationship type
- Filtering by month
- Sorting by age

---

## 16. Open Questions

- [ ] Should the "Coming up" / "Already celebrated" split be across the full list rather than per-page?
- [ ] Should `/list` use the user's configured timezone for the section split?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `ListCommandHandlerTest.java` | Empty list → `friends_empty`, birthday passed → "turned" label, birthday ahead → "turns" label, RU language, sort buttons in keyboard |

**Not covered by tests:**
- By-name sort
- Pagination (> 10 friends, ◀ / ▶ buttons, page indicator)
- Sort switch callback (`handleListSort`)
- Page navigation callback (`handleListPage`)
- Relationship label shown
- Birthday today → 🎂
- "Coming up" / "Already celebrated" section split
- Feb 29 in non-leap year

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
