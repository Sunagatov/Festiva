# Feature: Search Friends

> **Status:** `Stable`
> **Command:** `/search`
> **Handler:** `SearchCommandHandler.java`

---

## 1. Overview

Allows a user to find friends by name using a case-insensitive substring match.
Results show each matching friend's birthdate, zodiac sign, and days until their next birthday.
On no match, the search stays open for retry.

---

## 2. User Stories

- As a user, I want to search for a friend by name so that I can quickly find them without scrolling the full list.
- As a user, I want partial name matching so that I don't need to remember the exact spelling.

---

## 3. Functional Requirements

1. `/search` prompts the user to enter a search query.
2. Query is matched case-insensitively as a substring against all friend names.
3. Query must not be blank; blank input re-shows the prompt without changing state.
4. Query must not exceed 100 characters.
5. Results are sorted by day/month (same order as `/list`).
6. Each result shows: birthdate, zodiac sign, name, and days until next birthday (or 🎂 if today).
7. On no match, state stays open (`WAITING_FOR_SEARCH`) so the user can retry without re-running `/search`.
8. On match, state is cleared after showing results.
9. Original query casing is preserved in the results header.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Matching:** Case-insensitive, locale-root, substring match — not prefix-only, not fuzzy.
- **Query limit:** Max 100 characters (same as name limit).
- **Sorting:** Results sorted by `birthDate` day/month via `getFriendsSortedByDayMonth`.

---

## 5. Bot Flow

### Happy Path

```
User sends: /search
  → State: WAITING_FOR_SEARCH
  → Bot: "🔍 Enter a name to search:" [message key: search_prompt]

User sends: "ali"
  → Query matched case-insensitively against all friend names
  → State: IDLE
  → Bot: "🔍 Search results for "ali":
          – 15.03 ♈ Alice (in 45d)
          – 22.07 ♋ Alicia 🎂
          Use /search to search again or /edit to edit a friend." [message keys: search_results, search_results_hint]
```

### No Match

```
User sends: "xyz"
  → No friends match
  → State: WAITING_FOR_SEARCH (stays open)
  → Bot: "🔍 No friends found for "xyz". Try a different name, or tap /cancel to stop." [message key: search_none]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| Blank query | Empty or whitespace-only input | `search_prompt` re-shown; state unchanged |
| Query too long | Input > 100 characters | `search_too_long`; state unchanged |
| No match | Query matches no friend names | `search_none`; state stays `WAITING_FOR_SEARCH` |
| Birthday is today | `daysUntil = 0` | 🎂 shown instead of day count |

---

## 7. State Transitions

| From State | Event | To State |
|---|---|---|
| `IDLE` | `/search` command | `WAITING_FOR_SEARCH` |
| `WAITING_FOR_SEARCH` | Blank input | stays `WAITING_FOR_SEARCH` |
| `WAITING_FOR_SEARCH` | Query too long | stays `WAITING_FOR_SEARCH` |
| `WAITING_FOR_SEARCH` | Valid query, no match | stays `WAITING_FOR_SEARCH` |
| `WAITING_FOR_SEARCH` | Valid query, match found | `IDLE` |
| Any | `/cancel` | `IDLE` |

> States are defined in `BotState.java`

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Prompt | `search_prompt` | "🔍 Enter a name to search: *Tap /cancel at any time to stop.*" |
| Query too long | `search_too_long` | "⚠️ Search query is too long (max 100 characters). Please try a shorter name, or tap /cancel to stop." |
| No match | `search_none` | "🔍 No friends found for "%s". Try a different name, or tap /cancel to stop." |
| Results header | `search_results` | "🔍 **Search results for "%s":**" |
| Results hint | `search_results_hint` | "Use /search to search again or /edit to edit a friend." |

---

## 9. Acceptance Criteria

- [ ] Given `/search` is sent, then state is set to `WAITING_FOR_SEARCH` and prompt is shown.
- [ ] Given a blank query is submitted, then prompt is re-shown and state is unchanged.
- [ ] Given a query longer than 100 characters, then `search_too_long` is shown and state is unchanged.
- [ ] Given a query that matches a friend name (case-insensitive substring), then results are shown and state is cleared.
- [ ] Given a query with no matches, then `search_none` is shown and state stays `WAITING_FOR_SEARCH`.
- [ ] Given a friend's birthday is today, then 🎂 is shown instead of a day count.
- [ ] Given the query is `"ALI"`, then the results header shows `"ALI"` (original casing preserved).
- [ ] Given `/cancel` is sent, then state is cleared.
- [ ] Given the user's language is RU, then all messages are in Russian.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `name` | Matched against query (case-insensitive substring) |
| `birthDate` | Displayed in results; used to compute days until next birthday and zodiac |
| `telegramUserId` | Scopes the query to the current user |

---

## 11. Security & Privacy

- **Ownership:** Friends fetched by `telegramUserId` — users only search their own data.
- **Deletion:** No data written; nothing to delete.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

No structured log events for this feature.

---

## 13. Known Limitations

- **Substring match only** — no fuzzy matching, no diacritic-insensitive matching (e.g. `"Zoe"` does not match `"Zoë"`).
- **No pagination** — all matching results are returned in a single message. With 100 friends and a very short query (e.g. `"a"`), the message could be very long and hit Telegram's 4096-character message limit.
- **State stays open on no match** — convenient for retry, but if the user navigates away without `/cancel`, the state lingers until another command clears it.

---

## 14. Relationships to Other Features

- Data source: `add-friend`, `bulk-add`
- Results hint links to: `edit-friend`
- Related to: `list` (alternative way to browse all friends)

---

## 15. Out of Scope

- Fuzzy / phonetic matching
- Search by birthdate or relationship
- Paginated search results

---

## 16. Open Questions

- [ ] Should results be paginated to avoid hitting Telegram's message length limit?
- [ ] Should no-match keep state open (current behaviour) or clear state and require re-running `/search`?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `SearchCommandHandlerTest.java` | State set on `/search`, prompt shown, blank re-shows prompt, too-long error, no match keeps state open, match clears state, results hint, original casing preserved, RU language, friend name in results |

**Not covered by tests:**
- Birthday today → 🎂 shown instead of day count
- Multiple matches returned and sorted
- Very long result list approaching Telegram message limit
- Diacritic mismatch (e.g. `"Zoe"` vs `"Zoë"`)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
