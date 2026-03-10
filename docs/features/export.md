# Feature: Export Friends

> **Status:** `Stable`
> **Command:** `/export`
> **Handler:** `ExportCommandHandler.java`

---

## 1. Overview

Allows a user to download their full friends list as a `friends.csv` file.
The exported file uses the same format as `bulk-add` — it can be edited and re-uploaded via `/addmany`.

---

## 2. User Stories

- As a user, I want to export my friends list so that I have a backup.
- As a user, I want to export in CSV format so that I can edit it and re-import via `/addmany`.

---

## 3. Functional Requirements

1. `/export` sends a `friends.csv` file to the user via Telegram document message.
2. If the friend list is empty, bot replies with a text message instead of sending a file.
3. CSV format: `name,birthday,relationship` header row, one friend per line.
4. Date format: `DD.MM.YYYY`.
5. Relationship column: lowercase enum name (e.g. `friend`, `partner`). Empty string if no relationship set.
6. Names containing commas or double-quotes are RFC 4180 quoted: wrapped in `"`, internal `"` escaped as `""`.
7. Friends are sorted by day/month (same order as `/list`).
8. File is sent with a caption containing a `/addmany` hint.
9. If the Telegram API call fails, bot replies with an error message instead.
10. On success, `handle()` returns `null` — the file is sent directly via `TelegramClient`, not via the return value.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Encoding:** CSV file is UTF-8.
- **CSV quoting:** RFC 4180 compliant for names with commas or double-quotes.
- **Filename:** Always `friends.csv`.
- **Relationship format:** Lowercase enum name — matches the format accepted by `bulk-add` parser (case-insensitive).

---

## 5. Bot Flow

### Happy Path

```
User sends: /export
  → Bot fetches friends sorted by day/month
  → Bot sends: friends.csv file with caption "Your friends list. You can edit and upload it back with /addmany." [message key: export_caption]
  → handle() returns null
```

### Empty List

```
User sends: /export with no friends
  → Bot: "👥 Nothing to export yet. Use /add to add your first friend, then come back here." [message key: export_empty]
```

### Send Failure

```
Telegram API throws TelegramApiException
  → Bot: "⚠️ Export failed. Please try again later." [message key: export_failed]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response |
|---|---|---|
| No friends | Empty friend list | `export_empty` text message |
| Name contains comma | e.g. `Smith, John` | Quoted: `"Smith, John"` in CSV |
| Name contains double-quote | e.g. `O"Brien` | Quoted: `"O""Brien"` in CSV |
| No relationship set | `relationship = null` | Empty string in relationship column |
| Telegram API failure | `TelegramApiException` thrown | `export_failed` text message |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No friends | `export_empty` | "👥 Nothing to export yet. Use /add to add your first friend, then come back here." |
| Send failure | `export_failed` | "⚠️ Export failed. Please try again later." |
| File caption | `export_caption` | "Your friends list. You can edit and upload it back with /addmany." |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends, when `/export` is sent, then a `friends.csv` file is sent via Telegram.
- [ ] Given the user has no friends, when `/export` is sent, then bot replies with `export_empty`.
- [ ] Given the exported CSV, then it has a `name,birthday,relationship` header row.
- [ ] Given a friend with relationship `FRIEND`, then the CSV contains `friend` (lowercase).
- [ ] Given a friend with no relationship, then the relationship column is empty.
- [ ] Given a friend name containing a comma, then the name is quoted in the CSV.
- [ ] Given a friend name containing a double-quote, then the name is RFC 4180 escaped.
- [ ] Given the Telegram API throws, then bot replies with `export_failed`.
- [ ] Given the file is sent, then the caption contains a `/addmany` hint.
- [ ] Given the user's language is RU, then `export_empty` is in Russian.

---

## 10. Data Model

Read-only. No writes.

| Field Read | Notes |
|---|---|
| `name` | Written to CSV column 1; quoted if contains `,` or `"` |
| `birthDate` | Written to CSV column 2 as `DD.MM.YYYY` |
| `relationship` | Written to CSV column 3 as lowercase enum name; empty if null |

> Collection: `friends`. Sorted by `birthDate` day/month via `getFriendsSortedByDayMonth`.

---

## 11. Security & Privacy

- **Ownership:** Friends fetched by `telegramUserId` — users only export their own data.
- **Exposure:** The CSV file is sent only to the requesting user's chat. No server-side storage of the file.
- **Sensitive fields:** `notifyEnabled` and `telegramUserId` are not included in the export.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `export.failed` | `ERROR` | `userId`, `message` |

---

## 13. Known Limitations

- **`notifyEnabled` not exported** — if a user exports and re-imports, all friends will have `notifyEnabled=true` (the default). Per-friend mute settings are lost.
- **`handle()` returns `null` on success** — unusual pattern; the file is sent as a side effect via `TelegramClient.execute()`. If the caller expects a non-null `SendMessage`, this could cause a NPE.
- **No file size limit** — a user with 100 friends generates a small file, but there is no explicit cap on CSV size.

---

## 14. Relationships to Other Features

- Inverse of: `bulk-add` (same CSV format)
- Data source: `add-friend`, `bulk-add`, `edit-friend`
- Affected by: `remove-friend`, `delete-account`

---

## 15. Out of Scope

- Exporting in formats other than CSV (e.g. JSON, vCard)
- Including `notifyEnabled` in the export
- Scheduled/automatic export

---

## 16. Open Questions

- [ ] Should `notifyEnabled` be included in the export so that mute settings survive a re-import?
- [ ] Should the `handle()` return a success `SendMessage` instead of `null`, for consistency with other handlers?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `ExportCommandHandlerTest.java` | Empty list, file sent + null return, CSV header + relationship, `/add` hint, RU language, `/addmany` caption hint, `TelegramApiException` → `export_failed`, name with comma quoted |

**Not covered by tests:**
- Name with double-quote (`O"Brien` → `"O""Brien"`)
- Friend with no relationship (empty column)
- All 100 friends exported (max list)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
