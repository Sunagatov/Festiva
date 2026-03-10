# Feature: Import from Google Calendar (.ics)

> **Status:** `In Progress`
> **Command:** `/importics`
> **Handlers:** `ImportIcsCommandHandler.java` (to be created)
> **Interface:** implements `StatefulCommandHandler` — must implement `handledStates()` returning `Set.of(WAITING_FOR_ICS_FILE)`

---

## 1. Overview

Allows a user to upload a `.ics` file exported from Google Calendar (or any iCalendar-compatible app).
The bot parses the file, extracts events that repeat yearly (`RRULE:FREQ=YEARLY`) — treating them as birthdays — and bulk-imports them as friends.

Because Google Calendar birthday events often have summaries like `"День рождения Даши"` or `"Birthday of John"` instead of just the person's name, the bot uses an optional AI step (LangChain4j + OpenAI `gpt-4o-mini`) to extract the clean name from the summary. If AI is disabled or unavailable, the raw `SUMMARY` value is used as-is.

---

## 2. User Stories

- As a user who tracks birthdays in Google Calendar, I want to upload my `.ics` export so that I don't have to re-enter every birthday manually.
- As a user, I want to see a preview of what will be imported before it is saved.
- As a user, I want to know which events were skipped and why.

---

## 3. Functional Requirements

1. `/importics` prompts the user to upload a `.ics` file.
2. Only `.ics` files are accepted. The file download reuses the same MIME check as `BulkAddCommandHandler`: accepts any `text/*` (which includes `text/calendar`) and `application/octet-stream`. Files with other MIME types are rejected.
3. Max file size: **512 KB**.
4. The bot parses the file and extracts `VEVENT` blocks where `RRULE` contains `FREQ=YEARLY`.
5. From each qualifying event:
   - **Name** → extracted from `SUMMARY` via AI (see §16 AI Name Extraction). Falls back to raw `SUMMARY` if AI is disabled or fails.
   - **Date** → `DTSTART` property. Supports both `DTSTART;VALUE=DATE:YYYYMMDD` and `DTSTART:YYYYMMDDTHHMMSSZ`.
6. Events without `SUMMARY`, without `DTSTART`, or without `RRULE:FREQ=YEARLY` are silently skipped — they do **not** appear in the preview.
7. Extracted entries go through the **same validation pipeline as bulk-add** by calling `BulkAddParser.parse()` directly. To do this, the ICS parser converts each extracted event into a synthetic CSV line `"Name,DD.MM.YYYY"` — note the date must be reformatted from ICS `YYYYMMDD` to `DD.MM.YYYY` before building the line — and passes the list to `BulkAddParser.parse(lines, existingNames, lang)`.
   - Name: not blank, max 100 chars, unique per user (case-insensitive), not duplicate within batch.
   - Date: must not be in the future.
   - Total friends after import must not exceed cap of **100**.
   - Max **50 entries** per submission.
8. Bot shows a **preview** of valid and invalid yearly entries with a Confirm / Cancel inline keyboard before saving.
9. On confirm, all valid entries are saved. On cancel, nothing is saved.
10. After saving, state is cleared and a summary is shown (N added).
11. Pending valid entries are stored in a new `List<Friend> pendingIcsImport` field on `UserStateService.UserSession` — this field does not exist today and must be added. Bulk-add has no equivalent (it saves immediately without a pending step).

---

## 4. Non-Functional Requirements

- File is parsed in-memory; not written to disk.
- Parsing must be tolerant of CRLF and LF line endings.
- ICS line folding (lines starting with a space/tab are continuations) must be unfolded before parsing.
- No external ICS library — parse with simple line-by-line logic (the subset needed is small).
- AI name extraction is optional — controlled by `ai.enabled` property. When disabled, raw `SUMMARY` is used.
- All messages must exist in `messages_en.properties` and `messages_ru.properties`.

---

## 5. Bot Flow

```
User: /importics
Bot: "📅 Send me your .ics file exported from Google Calendar."
  → State: WAITING_FOR_ICS_FILE

User uploads: birthdays.ics
Bot: "Found 4 yearly events. Preview:
  ✅ Alice — 15 Mar 1990
  ✅ Bob — 22 Jul 1985
  ❌ Charlie — already in your list
  ❌ Work Anniversary — date is in the future
  (non-yearly events are silently ignored — not shown here)
  
  Save 2 friends?" + [✅ Confirm (2)] [❌ Cancel]
  → State: WAITING_FOR_ICS_CONFIRM

User taps: Confirm
Bot (edits preview message): "✅ Added 2 friend(s) from your calendar."
  → State: IDLE
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Behaviour |
|----------|-----------|
| No `FREQ=YEARLY` events found | "No yearly events found in this file." — state cleared |
| All events fail validation | Show preview with all errors, no Confirm button |
| File is not valid ICS | "Couldn't read the file. Please export from Google Calendar and try again." |
| File too large (> 512 KB) | "File is too large (max 512 KB)." |
| Wrong file type | "Please send a .ics file." |
| User sends text instead of file | "Please send a .ics file, or tap /cancel to stop." |
| Friend cap would be exceeded | Same as bulk-add: truncate valid entries, show warning |
| Over 50 valid entries | Same as bulk-add: process first 50, show warning |
| User sends text while in `WAITING_FOR_ICS_CONFIRM` | `CommandRouter` finds no `statefulHandler` for that state → sends `USE_BUTTONS` message (existing behaviour, no code change needed) |
| User taps Cancel on preview | "Import cancelled." — nothing saved, state cleared |
| `/cancel` at any step | State cleared, standard cancel message |

---

## 7. State Transitions

| From | Event | To |
|------|-------|----|
| `IDLE` | `/importics` | `WAITING_FOR_ICS_FILE` |
| `WAITING_FOR_ICS_FILE` | Valid `.ics` file received | `WAITING_FOR_ICS_CONFIRM` |
| `WAITING_FOR_ICS_FILE` | Invalid file / text | stays `WAITING_FOR_ICS_FILE` |
| `WAITING_FOR_ICS_CONFIRM` | Confirm callback | `IDLE` |
| `WAITING_FOR_ICS_CONFIRM` | Cancel callback | `IDLE` |
| Any | `/cancel` | `IDLE` |

> **Required:** Add to `BotState.java`: `WAITING_FOR_ICS_FILE`, `WAITING_FOR_ICS_CONFIRM`
> **Required:** Add `List<Friend> pendingIcsImport` to `UserStateService.UserSession` with getter/setter and clear it in `clearState()`.
> **Required:** `WAITING_FOR_ICS_CONFIRM` is handled via callback — add `ICS_CONFIRM` and `ICS_CANCEL` cases to `dispatchMisc()` in `CallbackQueryHandler`. Handlers return a `CallbackResult` (the package-private inner class of `CallbackQueryHandler`). The confirm handler saves `pendingIcsImport` and clears state; the cancel handler just clears state. Both return text-only `CallbackResult` (no markup). The response is delivered as an **edit of the preview message** (this is how all callbacks work in `BirthdayBot.consume()`).
> **Required:** Add `/importics` to the hardcoded command list in `BirthdayBot.start()` for both EN and RU.

---

## 8. Acceptance Criteria

- [ ] `/importics` sets state to `WAITING_FOR_ICS_FILE` and prompts for file.
- [ ] Uploading a valid `.ics` with yearly events shows a preview with Confirm/Cancel.
- [ ] Only `RRULE:FREQ=YEARLY` events are extracted; others are silently ignored.
- [ ] `DTSTART;VALUE=DATE:19900315` is parsed as `1990-03-15`.
- [ ] `DTSTART:19900315T000000Z` is parsed as `1990-03-15`.
- [ ] Duplicate names (within batch and against existing friends) are shown as errors in preview.
- [ ] Future dates are shown as errors in preview.
- [ ] Tapping Confirm saves valid entries and shows summary.
- [ ] Tapping Cancel saves nothing and clears state.
- [ ] File > 512 KB is rejected with an error message.
- [ ] Non-`.ics` file is rejected with an error message.
- [ ] `/cancel` at any step clears state.
- [ ] Friend cap is enforced — excess entries not saved.

---

## 9. Data Model

No new fields. Imported friends use the same `Friend` document:

| Field | Value |
|-------|-------|
| `telegramUserId` | from session |
| `name` | from `SUMMARY` |
| `birthDate` | from `DTSTART` |
| `relationship` | `null` (not in ICS) |
| `notifyEnabled` | `true` |

Pending import entries stored in a new `List<Friend> pendingIcsImport` field on `UserStateService.UserSession`. Note: bulk-add does **not** use a pending list — it saves immediately. This field must be added.

---

## 10. Security & Privacy

- File is downloaded via Telegram Bot API using the same pattern as `BulkAddCommandHandler`: `"https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath()`. Token must not be logged.
- File size capped at 512 KB to prevent memory exhaustion.
- Parsed names go through the same sanitisation as bulk-add (trim, length check).
- No ICS data is persisted beyond the current session.

---

## 11. Metrics & Observability

| Event | Level | Fields |
|-------|-------|--------|
| `ics.import.done` | `DEBUG` | `userId`, `added`, `skipped` |
| `ics.import.file_too_large` | `WARN` | `size` |
| `ics.import.invalid_file` | `WARN` | — |
| `ics.import.no_yearly_events` | `DEBUG` | `userId` |

---

## 12. Known Limitations

- Relationship is not imported (ICS has no standard field for it — user can edit after import).
- Only `FREQ=YEARLY` recurrence is recognised. `FREQ=YEARLY;BYMONTH=3` and other extended rules are treated the same (date is taken from `DTSTART`).
- ICS files with very complex folding or non-UTF-8 encoding may fail silently.
- No support for multi-file upload.

---

## 13. Error Messages

New constants must be added to `Messages.java` and corresponding keys to both `messages_en.properties` and `messages_ru.properties`.

| Java Constant | Property Key | EN Text |
|---------------|-------------|--------|
| `ICS_PROMPT` | `ics_prompt` | "📅 Send me your .ics file exported from Google Calendar, or tap /cancel to stop." |
| `ICS_NOT_A_FILE` | `ics_not_a_file` | "Please send a .ics file, or tap /cancel to stop." |
| `ICS_WRONG_TYPE` | `ics_wrong_type` | "Please send a .ics file (not a photo or other file type), or tap /cancel to stop." |
| `ICS_TOO_LARGE` | `ics_too_large` | "⚠️ File is too large (max 512 KB). Please export a smaller calendar." |
| `ICS_PARSE_ERROR` | `ics_parse_error` | "⚠️ Couldn't read the file. Please export from Google Calendar and try again." |
| `ICS_NO_EVENTS` | `ics_no_events` | "No yearly recurring events found in this file." |
| `ICS_PREVIEW` | `ics_preview` | "Found %d yearly event(s). Preview:\n\n%s\n\nSave %d friend(s)?" |
| `ICS_PREVIEW_NO_VALID` | `ics_preview_no_valid` | "Found %d yearly event(s), but none can be imported:\n\n%s" |
| `ICS_CONFIRM_BTN` | `ics_confirm_btn` | "✅ Confirm (%d)" |
| `ICS_DONE` | `ics_done` | "✅ Added %d friend(s) from your calendar." |
| `ICS_CANCELLED` | `ics_cancelled` | "Import cancelled." |

---

## 14. Relationships to Other Features

- Reuses bulk-add validation logic (`BulkAddParser` or extracted shared validator).
- Imported friends appear in `/list`, `/today`, `/upcomingbirthdays`, reminders — same as any other friend.
- Inverse of `/export` (which produces CSV, not ICS).

---

## 15. Out of Scope

- Exporting to `.ics` format.
- Syncing with Google Calendar (two-way sync).
- Importing non-birthday events (meetings, holidays, etc.).
- Updating existing friends from ICS (import is additive only).

---

## 16. AI Name Extraction

Google Calendar exports birthday event summaries in natural language (e.g. `"День рождения Юли"`, `"Birthday of John"`, `"Happy birthday!"`). Stripping these prefixes reliably across languages is not feasible with regex.

**Approach:** Use LangChain4j + OpenAI `gpt-4o-mini` (same stack as Iced-Latte) to extract just the person's name.

**Implementation plan:**
1. Add `langchain4j-open-ai` dependency to `pom.xml` (version `1.11.0`, same as Iced-Latte).
2. Add `OPENAI_API_KEY` env var to `.env` and `.env.prod`.
3. Create `IcsNameExtractorService` interface with a single method annotated with `@SystemMessage`:
   > *"Extract only the person's name from a birthday event title. Return just the name, nothing else. If no name can be identified, return the original text unchanged."*
4. Create `AiIcsNameExtractorConfig` annotated with `@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")` — wires `OpenAiChatModel` + `AiServices`.
5. In `ImportIcsCommandHandler`, inject `Optional<IcsNameExtractorService>` — call it per SUMMARY if present, otherwise use raw SUMMARY.

**Env vars:**

| Variable | Required | Description |
|---|---|---|
| `OPENAI_API_KEY` | only if `ai.enabled=true` | OpenAI API key |
| `ai.enabled` | ❌ | `true` to enable AI name extraction (default: `false`) |
| `ai.model-name` | ❌ | Defaults to `gpt-4o-mini` |
| `ai.base-url` | ❌ | Defaults to `https://api.openai.com/v1` |

**Fallback behaviour:** If AI is disabled, unavailable, or throws, the raw `SUMMARY` is used — import never fails due to AI.

---

## 17. Open Questions

- [ ] Should the bot attempt to detect the year-of-birth from the `DTSTART` year, or always use it as-is? (Google Calendar stores the actual birth year in `DTSTART` for birthday events.)
- [ ] Should relationship be guessable from the event title? (e.g. "Mom's Birthday" → `MUM`) — probably out of scope for v1.
- [ ] Should we support `.ics` pasted as text (not a file) for power users?

---

## 18. Testing Notes

**To be created:** `ImportIcsCommandHandlerTest.java`

Should cover:
- Valid file with mixed yearly/non-yearly events
- `DTSTART;VALUE=DATE` parsing
- `DTSTART` datetime parsing
- Duplicate detection (within batch and against DB)
- Future date rejection
- File too large
- No yearly events found
- Confirm flow saves entries
- Cancel flow saves nothing

---

## 19. Changelog

| Date | Change |
|------|--------|
| 2025 | Initial spec — Draft |
| 2026 | Added AI name extraction section (§16) — LangChain4j + OpenAI gpt-4o-mini, optional via `ai.enabled` |
