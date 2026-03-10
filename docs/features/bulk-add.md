# Feature: Bulk Add Friends

> **Status:** `Stable`
> **Command:** `/addmany`
> **Handlers:** `BulkAddCommandHandler.java`, `BulkAddParser.java`

---

## 1. Overview

Allows a user to add multiple friends at once by either pasting CSV-formatted text or uploading a `.csv` file.
Partial success is supported — valid entries are saved even if some lines have errors.

---

## 2. User Stories

- As a user, I want to add many friends at once so that I don't have to use `/add` repeatedly.
- As a user, I want to upload a CSV file so that I can prepare my friends list outside of Telegram.
- As a user, I want to get a CSV template so that I know the correct format.
- As a user, I want to see which lines failed and why so that I can fix and retry them.

---

## 3. Functional Requirements

1. User chooses between **paste text** or **get CSV template** (to then upload).
2. Input format per line: `Name,DD.MM.YYYY` or `Name,DD.MM.YYYY,RELATIONSHIP`.
3. A header row (`name,birthday,...`) is automatically detected and skipped.
4. Maximum **50 entries** per submission (`BulkAddParser.MAX_ENTRIES`). Entries beyond 50 are silently dropped after a warning.
5. Valid entries are saved even when some lines contain errors (partial success).
6. Total friends after import must not exceed the cap of **100**. Excess valid entries are truncated; a warning is added to the response.
7. Relationship field is optional. If blank, friend is saved without a relationship.
8. If relationship value is unrecognised, the friend is still saved without a relationship and a warning is added.
9. Duplicate names within the same batch are rejected per duplicate line.
10. Names already existing in the user's friend list are rejected.
11. After processing, state is cleared regardless of outcome.

---

## 4. Non-Functional Requirements

- **i18n:** All messages must exist in `messages_en.properties` and `messages_ru.properties`.
- **Date format:** `DD.MM.YYYY` only (e.g. `15.03.1990`). No other formats accepted.
- **Relationship format:** Must match `Relationship` enum name exactly (case-insensitive, e.g. `FRIEND`, `friend`).
- **File constraints:** Only `text/*` or `application/octet-stream` MIME types accepted. Max file size: **512 KB**.
- **Encoding:** Files are read as UTF-8.
- **CSV quoting:** Quoted names (`"Alice"`) are supported; double-quote escaping (`""`) inside quoted names is handled.
- **Name limits:** Max 100 characters, must not be blank.

---

## 5. Bot Flow

### Happy Path — Paste Text

```
User sends: /addmany
  → Bot: "How would you like to add friends?" + [📋 Paste text] [📥 Get CSV template] [message key: bulk_add_choose]

User taps: "📋 Paste text"
  → State: WAITING_FOR_BULK_ADD
  → Bot: "📋 Bulk add friends — send one friend per line: Name,DD.MM.YYYY" [message key: bulk_add_prompt]

User sends:
  Alice,15.03.1990
  Bob,22.07.1985,FRIEND

  → Parser validates all lines
  → Valid friends saved to DB
  → State: IDLE
  → Bot: "✅ Added 2 friend(s) successfully." [message key: bulk_add_success]
```

### Happy Path — CSV File

```
User sends: /addmany
  → Bot: "How would you like to add friends?" + [📋 Paste text] [📥 Get CSV template]

User taps: "📥 Get CSV template"
  → Bot sends file: friends_template.csv with caption [message key: bulk_add_csv_caption]

User fills in the file and uploads it back
  → State must be WAITING_FOR_BULK_ADD (user must have tapped "Paste text" first, or re-entered /addmany)
  → Same processing as paste text
```

### Partial Success

```
User sends 3 lines, 1 has an invalid date:
  → 2 friends saved
  → Bot: "✅ Added 2 friend(s)." + "⚠️ Errors in 1 line(s): • Line 2 (Bob): invalid date..."
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| No valid entries, no data at all | Empty message or only blank lines | `bulk_error_no_data` |
| All lines have errors | Every line fails validation | `bulk_add_errors` only (no success message) |
| Over 50 entries submitted | More than 50 non-blank lines | `bulk_error_too_many` warning added; first 50 processed |
| Friend cap would be exceeded | Valid entries + existing > 100 | `bulk_cap_exceeded` warning; only allowed entries saved |
| File with unsupported MIME type | e.g. PDF uploaded | `bulk_add_file_invalid` |
| File too large | File > 512 KB | `bulk_add_file_invalid` |
| File download fails | Network/Telegram API error | `bulk_add_file_invalid` |
| Non-text, non-document message | e.g. photo sent | `bulk_add_file_invalid` |
| Unknown relationship value | e.g. `BESTIE` | Friend saved without relationship + `bulk_error_relationship_invalid` warning |
| Duplicate name in batch | Same name appears twice in submission | `bulk_error_duplicate` for second occurrence |
| Name already exists in DB | Name matches existing friend (case-insensitive) | `bulk_error_exists` |
| Blank name | Empty first column | `bulk_error_name_empty` |
| Name too long | Name > 100 chars | `bulk_error_name_long` |
| Invalid date format | Not `DD.MM.YYYY` | `bulk_error_date_invalid` |
| Future date | Date is after today | `bulk_error_date_future` |

---

## 7. State Transitions

| From State | Event | To State |
|---|---|---|
| `IDLE` | `/addmany` command | `IDLE` (choice keyboard shown, no state change yet) |
| `IDLE` | `BULK_PASTE` callback | `WAITING_FOR_BULK_ADD` |
| `WAITING_FOR_BULK_ADD` | Text or file received | `IDLE` |
| Any | `/cancel` | `IDLE` |

> Note: Tapping "📥 Get CSV template" does NOT change state — it only sends the template file.
> The user must tap "📋 Paste text" (or re-run `/addmany`) to enter `WAITING_FOR_BULK_ADD`.

> States are defined in `BotState.java`

---

## 8. Acceptance Criteria

- [ ] Given the user has fewer than 100 friends, when `/addmany` is sent, then bot shows the choice keyboard.
- [ ] Given the user taps "Paste text", then state changes to `WAITING_FOR_BULK_ADD` and bot shows the prompt.
- [ ] Given the user taps "Get CSV template", then bot sends `friends_template.csv` and state does NOT change.
- [ ] Given valid pasted text with 2 entries, when submitted, then both friends are saved and bot replies with `bulk_add_success`.
- [ ] Given a CSV file is uploaded while in `WAITING_FOR_BULK_ADD`, then it is processed identically to pasted text.
- [ ] Given one line is invalid and one is valid, when submitted, then the valid friend is saved and bot shows both success and error messages.
- [ ] Given all lines are invalid, when submitted, then no friends are saved and bot shows only `bulk_add_errors`.
- [ ] Given more than 50 lines, when submitted, then only the first 50 are processed and a `bulk_error_too_many` warning is shown.
- [ ] Given adding valid entries would exceed 100 friends, then only the allowed number are saved and `bulk_cap_exceeded` warning is shown.
- [ ] Given a PDF file is uploaded, then bot replies with `bulk_add_file_invalid`.
- [ ] Given `/cancel` is sent while in `WAITING_FOR_BULK_ADD`, then state is cleared.

---

## 9. Data Model

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | `String` | ✅ | MongoDB auto-generated `_id` |
| `telegramUserId` | `long` | ✅ | Scopes the friend to its owner |
| `name` | `String` | ✅ | Max 100 chars, unique per user (case-insensitive) |
| `birthDate` | `LocalDate` | ✅ | Format `DD.MM.YYYY`, must not be in the future |
| `relationship` | `Relationship` | ❌ | Enum, nullable. Matched case-insensitively from CSV column 3 |
| `notifyEnabled` | `boolean` | ✅ | Defaults to `true` |

> Collection: `friends` in MongoDB.

---

## 10. Security & Privacy

- **Ownership:** All queries are scoped by `telegramUserId` — users can only access their own friends.
- **Deletion:** All friend records for a user are removed on `/deleteaccount`.
- **Exposure:** `telegramUserId` must not appear in user-facing messages or bot responses.
- **File download:** The bot downloads uploaded files via the Telegram Bot API using the bot token in the URL — the token must never be logged.

---

## 11. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `bulk.add.done` | `DEBUG` | `userId`, `added`, `errors` |
| `bulk.csv.template.failed` | `WARN` | `chatId` |
| `bulk.add.file.unsupported.mime` | `WARN` | `mime` |
| `bulk.add.file.too.large` | `WARN` | `size` |
| `bulk.add.file.download.failed` | `WARN` | — |
| `bulk.parse.date.invalid` | `DEBUG` | `line`, `value` |
| `bulk.parse.unknown.relationship` | `DEBUG` | `line`, `value` |

---

## 12. Known Limitations

- Uploading a CSV file requires the user to first tap "Paste text" to enter `WAITING_FOR_BULK_ADD` state. Uploading directly after tapping "Get CSV template" silently does nothing.
- The 50-entry limit is hardcoded in `BulkAddParser.MAX_ENTRIES` — not configurable.
- Friend cap (100) is hardcoded in `FriendService.FRIEND_CAP` — not configurable.

---

## 13. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| No data | `bulk_error_no_data` | "⚠️ No data found. Send at least one line in the format: `Name,DD.MM.YYYY`, or tap /cancel to stop." |
| Too many entries | `bulk_error_too_many` | "⚠️ Too many entries (%d submitted, max %d). Only the first %d will be processed." |
| Cap exceeded | `bulk_cap_exceeded` | "⚠️ Only %d friend(s) added — friend limit of %d reached. Use /remove to free up a spot." |
| Invalid file | `bulk_add_file_invalid` | "⚠️ Couldn't read the file. Please send a plain text or .csv file, or tap /cancel to stop." |
| No valid entries | `bulk_add_empty` | "⚠️ No valid entries found. Check the format: `Name,DD.MM.YYYY` and try again, or tap /cancel to stop." |
| Success | `bulk_add_success` | "✅ Added %d friend(s) successfully. Use /list to view your friends or /add to add one more." |
| Errors summary | `bulk_add_errors` | "⚠️ Errors in %d line(s): %s Fix the errors and use /addmany to try again." |
| Invalid format | `bulk_error_format` | "Line %d: invalid format — expected \"Name,DD.MM.YYYY\"" |
| Blank name | `bulk_error_name_empty` | "Line %d: name is empty" |
| Name too long | `bulk_error_name_long` | "Line %d: name too long (max 100 characters)" |
| Invalid date | `bulk_error_date_invalid` | "Line %d (%s): invalid date \"%s\" — use DD.MM.YYYY" |
| Future date | `bulk_error_date_future` | "Line %d (%s): birth date cannot be in the future" |
| Already exists | `bulk_error_exists` | "Line %d (%s): already exists" |
| Duplicate in batch | `bulk_error_duplicate` | "Line %d (%s): duplicate in this batch" |
| Unknown relationship | `bulk_error_relationship_invalid` | "Line %d (%s): unknown relationship \"%s\" — ignored, saved without relationship" |

---

## 14. Relationships to Other Features

- Affects: `/list`, `/today`, `/upcomingbirthdays`, `/jubilee`, `/stats`, `/export`, birthday reminders
- Shares friend cap logic with: `add-friend`
- Inverse of: `export` (export produces the same CSV format that bulk-add consumes)

---

## 15. Out of Scope

- Adding a single friend interactively → see `add-friend` feature
- Editing existing friends via CSV upload (import is additive only, no updates)
- Deleting friends via CSV

---

## 16. Open Questions

- [ ] Should uploading a CSV file work without first tapping "Paste text"? Currently the user must be in `WAITING_FOR_BULK_ADD` state to submit a file, which is only set via the paste button — the CSV template flow has no state entry point of its own.
- [ ] Should unknown relationship values be a hard error (line rejected) rather than a soft warning (friend saved without relationship)?
- [ ] Should the 50-entry limit be configurable via environment variable?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `BulkAddCommandHandlerTest.java` | Paste text flow, empty input, header-only CSV, valid entries saved, partial success, friend cap enforcement, RU language |
| `BulkAddParserTest.java` | All validation rules: blank name, name too long, duplicate in batch, existing name, invalid date format, future date, missing comma, over-limit entries, relationship parsing, unknown relationship, mixed valid/invalid rows, RU errors |

**Not covered by tests:**
- CSV file upload flow (document handling, MIME type check, file size limit, download failure)
- `sendCsvTemplate` success path
- `BULK_CSV` callback triggering template send

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
