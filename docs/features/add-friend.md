# Feature: Add Friend

> **Status:** `Stable`
> **Command:** `/add`
> **Handlers:** `AddFriendCommandHandler.java`, `DatePickerCallbackHandler.java`

---

## 1. Overview

Allows a user to add a single friend with a name, birthdate, and optional relationship label.
The birthdate is collected via an inline date picker (year → month → day).
The relationship is selected from a predefined list or skipped.

---

## 2. User Stories

- As a user, I want to add a friend with their birthdate so that I receive birthday reminders for them.
- As a user, I want to label my relationship with a friend so that reminders feel more personal.
- As a user, I want to skip the relationship label so that I can add a friend quickly.

---

## 3. Functional Requirements

1. User must not exceed the friend cap of **100 friends**.
2. Friend name must not be blank.
3. Friend name must not exceed **100 characters**.
4. Friend name must be unique per user (case-insensitive).
5. Birthdate must not be in the future.
6. Relationship is optional — user may skip it.
7. After a friend is added, the user is offered two quick actions: **List** and **Add another**.
8. The flow can be triggered both via `/add` command and via the `ACTION_ADD` inline callback button.

---

## 4. Non-Functional Requirements

- **i18n:** All messages must exist in `messages_en.properties` and `messages_ru.properties`.
- **Validation:** Name is trimmed before validation. Blank check happens after trim.
- **Limits:** Hard cap of 100 friends per user (`FriendService.FRIEND_CAP`). Cap is checked both at flow entry and at the moment of saving (race condition guard).
- **Leap year:** Feb 29 is supported. `Friend.nextBirthday()` skips to the next leap year when the current year is not a leap year.

---

## 5. Bot Flow

### Happy Path

```
User sends: /add
  → Bot checks friend cap
  → State: WAITING_FOR_ADD_FRIEND_NAME
  → Bot: "👤 Enter your friend's name:" [message key: enter_name]

User sends: "Alice"
  → Bot validates name
  → State: WAITING_FOR_ADD_FRIEND_DATE
  → Bot: "Select Alice's birth year:" + year picker keyboard [message key: date_pick_year]

User taps a year (e.g. 1990)
  → Bot: "Select Alice's birth month:" + month picker keyboard [message key: date_pick_month]

User taps a month (e.g. March)
  → Bot: "Select Alice's birth day:" + day picker keyboard [message key: date_pick_day]

User taps a day (e.g. 15)
  → State: WAITING_FOR_ADD_FRIEND_RELATIONSHIP
  → Bot: "Choose your relationship with Alice:" + relationship keyboard [message key: relationship_pick]

User taps a relationship (e.g. "👫 Friend") or taps "⏭ Skip"
  → Friend saved to DB
  → State: IDLE
  → Bot: "✅ Alice added!" + [📋 List] [➕ Add another] buttons [message key: friend_added]
```

### Navigation within Date Picker

| Action | Result |
|---|---|
| Tap "◄ Earlier" / "Later ►" on year picker | Paginate year keyboard |
| Tap "← Back" on month picker | Return to year picker |
| Tap "← Back" on day picker | Return to month picker |

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| Friend cap reached at `/add` | User already has 100 friends | `friend_cap` |
| Friend cap reached at save | Race condition — cap hit between entry and save | `friend_cap` |
| Name is blank | User sends empty or whitespace-only text | `name_empty` |
| Name too long | Name > 100 characters | `name_too_long` |
| Duplicate name | Name already exists (case-insensitive) | `name_exists` |
| Future birthdate | Selected date is after today | `date_future_error` (day picker stays open) |
| Session expired | Pending state lost (e.g. bot restart) | `session_expired` |
| User sends text while in date picker state | Non-text input during date selection | `use_buttons` |
| Triggered via inline button | `ACTION_ADD` callback | Same flow from name entry step |

---

## 7. State Transitions

| From State | Event | To State |
|---|---|---|
| `IDLE` | `/add` command or `ACTION_ADD` callback | `WAITING_FOR_ADD_FRIEND_NAME` |
| `WAITING_FOR_ADD_FRIEND_NAME` | Valid name submitted | `WAITING_FOR_ADD_FRIEND_DATE` |
| `WAITING_FOR_ADD_FRIEND_NAME` | Invalid name | stays `WAITING_FOR_ADD_FRIEND_NAME` |
| `WAITING_FOR_ADD_FRIEND_DATE` | Day selected and date is valid | `WAITING_FOR_ADD_FRIEND_RELATIONSHIP` |
| `WAITING_FOR_ADD_FRIEND_DATE` | Day selected but date is in future | stays `WAITING_FOR_ADD_FRIEND_DATE` |
| `WAITING_FOR_ADD_FRIEND_RELATIONSHIP` | Relationship selected or skipped | `IDLE` |
| Any | `/cancel` | `IDLE` |

> States are defined in `BotState.java`

---

## 8. Acceptance Criteria

- [ ] Given the user has fewer than 100 friends, when `/add` is sent, then bot asks for a name.
- [ ] Given the user has 100 friends, when `/add` is sent, then bot replies with `friend_cap` and does not change state.
- [ ] Given a valid name, when submitted, then bot shows the year picker.
- [ ] Given a blank name, when submitted, then bot replies with `name_empty` and stays in `WAITING_FOR_ADD_FRIEND_NAME`.
- [ ] Given a name longer than 100 characters, when submitted, then bot replies with `name_too_long`.
- [ ] Given a duplicate name (case-insensitive), when submitted, then bot replies with `name_exists`.
- [ ] Given a future date is selected, when day is tapped, then bot replies with `date_future_error` and day picker stays open.
- [ ] Given a valid date is selected, when day is tapped, then bot shows the relationship picker.
- [ ] Given relationship is skipped, when skip is tapped, then friend is saved without a relationship.
- [ ] Given the flow completes, then bot shows `friend_added` with List and Add another buttons.
- [ ] Given `/cancel` is sent at any step, then state is cleared and bot replies with cancel message.

---

## 9. Data Model

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | `String` | ✅ | MongoDB auto-generated `_id` |
| `telegramUserId` | `long` | ✅ | Scopes the friend to its owner |
| `name` | `String` | ✅ | Max 100 chars, unique per user (case-insensitive) |
| `birthDate` | `LocalDate` | ✅ | Must not be in the future |
| `relationship` | `Relationship` | ❌ | Enum, nullable |
| `notifyEnabled` | `boolean` | ✅ | Defaults to `true` |

> Collection: `friends` in MongoDB.

---

## 10. Security & Privacy

- **Ownership:** All queries are scoped by `telegramUserId` — users can only access their own friends.
- **Deletion:** All friend records for a user are removed on `/deleteaccount`.
- **Exposure:** `telegramUserId` must not appear in user-facing messages or bot responses.

---

## 11. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `friend.added` | `DEBUG` | `userId`, `name`, `relationship` |

---

## 12. Known Limitations

- Friend cap (100) is hardcoded in `FriendService.FRIEND_CAP` — not configurable via environment variable.

---

## 13. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Friend cap reached | `friend_cap` | "⚠️ You've reached the limit of **100** friends. Use /remove to free up a spot, then try again." |
| Blank name | `name_empty` | "⚠️ Name can't be empty. Please enter a name, or tap /cancel to stop." |
| Name too long | `name_too_long` | "⚠️ Name is too long (max 100 characters). Please enter a shorter name, or tap /cancel to stop." |
| Duplicate name | `name_exists` | "⚠️ A friend named "%s" already exists. Please enter a different name, or tap /cancel to stop." |
| Future date | `date_future_error` | "⚠️ Birth date can't be in the future. Please pick a past date, or tap /cancel to stop." |
| Session expired | `session_expired` | "⏰ This action has expired. Please start again with the relevant command." |
| Text sent during date picker | `use_buttons` | "👇 Please use the buttons above to continue, or tap /cancel to stop." |
| Friend added successfully | `friend_added` | "✅ %s added! Use /list to view friends or /add to add another." |

---

## 14. Relationships to Other Features

- Affects: `/list`, `/today`, `/upcomingbirthdays`, `/jubilee`, `/stats`, `/export`, birthday reminders
- Shares date picker UI with: `edit-friend` (date field editing)
- Shares relationship picker UI with: `edit-friend` (relationship field editing)

---

## 15. Out of Scope

- Adding multiple friends at once → see `bulk-add` feature
- Editing a friend after they are added → see `edit-friend` feature
- Notification preferences per friend (notify toggle) → see `edit-friend` feature

---

## 16. Open Questions

- [ ] Should duplicate name check be case-insensitive AND diacritic-insensitive? (e.g. "Zoë" vs "Zoe")
- [ ] Should there be a confirmation step before saving, or is the relationship selection sufficient as a final step?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `AddFriendCommandHandlerTest.java` | Friend cap check, name validation (blank, too long, duplicate), state transitions, RU language, `/cancel` hint |

**Not covered by tests:**
- Date picker navigation (back to year, back to month)
- Future date rejection via day picker
- Relationship selection and skip
- `ACTION_ADD` callback trigger
- Session expiry during date picker

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
