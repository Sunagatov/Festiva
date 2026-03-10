# Feature: Edit Friend

> **Status:** `Stable`
> **Command:** `/edit`
> **Handlers:** `EditFriendCommandHandler.java`, `EditCallbackHandler.java`, `DatePickerCallbackHandler.java`

---

## 1. Overview

Allows a user to edit an existing friend's name, birthdate, relationship label, or notification toggle.
The user selects a friend from a paginated list, then chooses which field to edit.
Name editing is text-based; date editing reuses the date picker; relationship editing reuses the relationship picker.

---

## 2. User Stories

- As a user, I want to fix a friend's name so that reminders show the correct name.
- As a user, I want to update a friend's birthdate so that reminders fire on the right day.
- As a user, I want to change a friend's relationship label so that it reflects our current relationship.
- As a user, I want to mute reminders for a specific friend so that I don't get notified for them.

---

## 3. Functional Requirements

1. `/edit` shows a paginated list of friends (10 per page) sorted by day/month.
2. If the friend list is empty, bot shows an empty state message.
3. User selects a friend → bot shows a field picker with 4 options: Name, Date, Relationship, Notifications toggle.
4. **Name edit:** user types a new name; same validation rules as `add-friend` apply, except the same name (case-insensitive) is allowed (renaming to same name is a no-op save).
5. **Date edit:** reuses the date picker (year → month → day); same future-date validation applies.
6. **Relationship edit:** reuses the relationship picker; user can change or clear the relationship.
7. **Notifications toggle:** immediately toggles `notifyEnabled` on the friend; no confirmation step.
8. All edits are identified by friend `id` (MongoDB `_id`), not by name — safe against concurrent renames.
9. After each edit, state is cleared and bot shows a success message with a `/edit` hint.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Pagination:** 10 friends per page (`EditFriendCommandHandler.PAGE_SIZE`). Prev/next navigation buttons shown only when needed.
- **ID-based lookup:** All callback operations use MongoDB `_id` to find the friend, not name. This prevents stale-name bugs.
- **Validation:** Name validation identical to `add-friend` (blank, max 100 chars, duplicate check with same-name exception).

---

## 5. Bot Flow

### Happy Path — Edit Name

```
User sends: /edit
  → Bot: "Select a friend to edit." + paginated friend list keyboard [message key: edit_select]

User taps a friend (e.g. "Alice (15.03.1990)")
  → Bot: "Edit Alice (15.03.1990) — what would you like to change?" + [📝 Name] [📅 Date] [💞 Relationship] [🔔 Notifs ON] [message key: edit_choose_field]

User taps: "📝 Name"
  → State: WAITING_FOR_EDIT_NAME
  → Bot: "Enter a new name for Alice:" [message key: edit_enter_name]

User sends: "Alicia"
  → Friend name updated in DB
  → State: IDLE
  → Bot: "✅ Name updated to Alicia!" [message key: edit_name_done]
```

### Happy Path — Edit Date

```
User taps: "📅 Date" on field picker
  → State: WAITING_FOR_EDIT_DATE
  → Bot: year picker keyboard [message key: date_pick_year]

User picks year → month → day
  → Friend birthdate updated in DB
  → State: IDLE
  → Bot: "✅ Birth date for Alice updated!" [message key: edit_date_done]
```

### Happy Path — Toggle Notifications

```
User taps: "🔔 Notifs ON" on field picker
  → Notification toggled immediately (no state change)
  → Bot: "🔔 Notifications for Alice: OFF 🔕." [message key: edit_notify_toggled]
  → Button label flips to "🔕 Notifs OFF" on next open
```

### Happy Path — Edit Relationship

```
User taps: "💞 Relationship" on field picker
  → State: WAITING_FOR_EDIT_RELATIONSHIP
  → Bot: relationship picker keyboard [message key: relationship_pick]

User taps a relationship or Skip
  → Friend relationship updated in DB
  → State: IDLE
  → Bot: "✅ Relationship updated for Alice!" [message key: edit_rel_done]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| No friends | `/edit` with empty list | `friends_empty` |
| Friend deleted between list and tap | Friend ID not found in DB | `session_expired` |
| Session expired during name edit | `pendingName` is null | `session_expired` |
| Blank new name | User sends empty/whitespace | `name_empty` |
| New name too long | Name > 100 chars | `name_too_long` |
| New name already taken | Another friend has that name | `name_exists` |
| Same name (case-insensitive) | User "renames" Alice → alice | Allowed — saved successfully |
| Future date selected | Day tapped is after today | `date_future_error` (day picker stays open) |
| Paginate friend list | > 10 friends | ◀ / ▶ navigation buttons shown |

---

## 7. State Transitions

| From State | Event | To State |
|---|---|---|
| `IDLE` | `/edit` command | `IDLE` (friend list shown, no state change) |
| `IDLE` | Friend selected (callback) | `IDLE` (field picker shown, no state change) |
| `IDLE` | "📝 Name" tapped | `WAITING_FOR_EDIT_NAME` |
| `IDLE` | "📅 Date" tapped | `WAITING_FOR_EDIT_DATE` |
| `IDLE` | "💞 Relationship" tapped | `WAITING_FOR_EDIT_RELATIONSHIP` |
| `IDLE` | "🔔 Notifs" tapped | `IDLE` (immediate toggle, no state change) |
| `WAITING_FOR_EDIT_NAME` | Valid name submitted | `IDLE` |
| `WAITING_FOR_EDIT_NAME` | Invalid name | stays `WAITING_FOR_EDIT_NAME` |
| `WAITING_FOR_EDIT_DATE` | Valid day selected | `IDLE` |
| `WAITING_FOR_EDIT_DATE` | Future day selected | stays `WAITING_FOR_EDIT_DATE` |
| `WAITING_FOR_EDIT_RELATIONSHIP` | Relationship selected or skipped | `IDLE` |
| Any | `/cancel` | `IDLE` |

> States are defined in `BotState.java`

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Empty friend list | `friends_empty` | "👥 **No friends yet.** Use /add to add your first one." |
| Session expired | `session_expired` | "⏰ This action has expired. Please start again with the relevant command." |
| Blank name | `name_empty` | "⚠️ Name can't be empty. Please enter a name, or tap /cancel to stop." |
| Name too long | `name_too_long` | "⚠️ Name is too long (max 100 characters). Please enter a shorter name, or tap /cancel to stop." |
| Duplicate name | `name_exists` | "⚠️ A friend named "%s" already exists. Please enter a different name, or tap /cancel to stop." |
| Future date | `date_future_error` | "⚠️ Birth date can't be in the future. Please pick a past date, or tap /cancel to stop." |
| Name updated | `edit_name_done` | "✅ Name updated to **%s**! Use /edit to make more changes." |
| Date updated | `edit_date_done` | "✅ Birth date for **%s** updated! Use /edit to make more changes." |
| Relationship updated | `edit_rel_done` | "✅ Relationship updated for **%s**! Use /edit to make more changes." |
| Notification toggled | `edit_notify_toggled` | "🔔 Notifications for **%s**: **%s**. Use /edit to make more changes." |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends, when `/edit` is sent, then bot shows a paginated friend list.
- [ ] Given the user has no friends, when `/edit` is sent, then bot shows `friends_empty`.
- [ ] Given the user selects a friend, then bot shows the field picker with Name, Date, Relationship, and Notifications buttons.
- [ ] Given the user taps Name, then state changes to `WAITING_FOR_EDIT_NAME` and bot asks for a new name.
- [ ] Given a valid new name is submitted, then friend name is updated and state is cleared.
- [ ] Given the same name (case-insensitive) is submitted, then it is accepted and saved.
- [ ] Given a duplicate name (different friend) is submitted, then bot replies with `name_exists`.
- [ ] Given the user taps Date, then state changes to `WAITING_FOR_EDIT_DATE` and year picker is shown.
- [ ] Given a valid date is selected, then friend birthdate is updated and state is cleared.
- [ ] Given the user taps Relationship, then state changes to `WAITING_FOR_EDIT_RELATIONSHIP` and relationship picker is shown.
- [ ] Given the user taps Notifications, then `notifyEnabled` is toggled immediately with no state change.
- [ ] Given the friend is deleted between list display and selection, then bot replies with `session_expired`.
- [ ] Given more than 10 friends exist, then pagination buttons are shown.
- [ ] Given `/cancel` is sent at any step, then state is cleared.

---

## 10. Data Model

Edits the `friends` collection. No new documents created.

| Field Modified | Type | Notes |
|---|---|---|
| `name` | `String` | Max 100 chars, unique per user (case-insensitive) |
| `birthDate` | `LocalDate` | Must not be in the future |
| `relationship` | `Relationship` | Enum, nullable — can be cleared via Skip |
| `notifyEnabled` | `boolean` | Toggled in-place |

> Lookup is always by `id` (`_id` in MongoDB), never by name.

---

## 11. Security & Privacy

- **Ownership:** `findFriendById` returns the friend regardless of owner — ownership is implicitly trusted via the Telegram session. A malicious user crafting a callback with another user's friend ID could theoretically edit it.
- **Deletion:** Friend data is removed on `/deleteaccount`.
- **Exposure:** `telegramUserId` and MongoDB `id` must not appear in user-facing messages.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `friend.date.updated` | `DEBUG` | `userId`, `name` |
| `friend.relationship.updated` | `DEBUG` | `userId`, `name` / `id`, `rel` |

> Name update and notify toggle have no structured log events currently.

---

## 13. Known Limitations

- **Ownership not enforced on edit:** `findFriendById` does not verify `telegramUserId` matches the caller. A crafted callback with another user's friend ID would succeed. (Compare: `handleRemove` in `CallbackQueryHandler` does check ownership.)
- **No log event for name update or notify toggle** — gaps in observability.
- **Pagination page size (10) is hardcoded** in `EditFriendCommandHandler.PAGE_SIZE`.

---

## 14. Relationships to Other Features

- Shares date picker UI with: `add-friend`
- Shares relationship picker UI with: `add-friend`
- Shares name validation logic with: `add-friend`
- Affects: `/list`, `/today`, `/upcomingbirthdays`, `/jubilee`, `/stats`, `/export`, birthday reminders

---

## 15. Out of Scope

- Editing multiple friends at once
- Bulk editing via CSV
- Deleting a friend → see `remove-friend` feature

---

## 16. Open Questions

- [ ] Should ownership be enforced in `findFriendById` calls within edit callbacks? (see Known Limitations)
- [ ] Should a successful edit return the user to the field picker (to allow multiple edits in one session) rather than clearing state entirely?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `EditFriendCommandHandlerTest.java` | Empty list, session expired, blank/long/duplicate name, same-name case-insensitive, valid rename, state cleared, RU language |
| `EditCallbackHandlerTest.java` | Friend selection, field picker buttons, name/date/notify callbacks, not-found → session expired, notify toggle, RU language |

**Not covered by tests:**
- Date edit flow end-to-end (date picker → `WAITING_FOR_EDIT_DATE` → save)
- Relationship edit flow end-to-end
- Pagination (> 10 friends, ◀ / ▶ buttons)
- Ownership vulnerability (cross-user edit via crafted callback)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
