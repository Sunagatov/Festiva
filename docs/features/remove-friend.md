# Feature: Remove Friend

> **Status:** `Stable`
> **Command:** `/remove`
> **Handlers:** `RemoveCommandHandler.java`, `CallbackQueryHandler.java` (`handleRemove`, `handleConfirmRemove`, `handleCancelRemove`)

---

## 1. Overview

Allows a user to permanently delete a friend from their list.
The user selects a friend from a paginated list, then confirms or cancels the deletion.
Deletion is irreversible — there is no undo.

---

## 2. User Stories

- As a user, I want to remove a friend so that I stop receiving reminders for them.
- As a user, I want a confirmation step so that I don't accidentally delete a friend.
- As a user, I want to cancel the removal so that I can change my mind safely.

---

## 3. Functional Requirements

1. `/remove` shows a paginated list of friends (10 per page) sorted by day/month.
2. If the friend list is empty, bot shows an empty state with an "➕ Add a friend" button.
3. User selects a friend → bot shows a confirmation prompt with ✅ Yes / ❌ No buttons.
4. On confirm: friend is permanently deleted from DB; state is cleared.
5. On cancel: no deletion occurs; state is cleared.
6. Deletion is by friend `id` (MongoDB `_id`) — safe against stale names.
7. Ownership is verified before deletion — friend's `telegramUserId` must match the caller.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Pagination:** 10 friends per page (`RemoveCommandHandler.PAGE_SIZE`). Prev/next buttons shown only when needed.
- **Irreversibility:** No soft-delete or undo. Deletion is immediate and permanent.
- **Ownership check:** `handleConfirmRemove` verifies `friend.getTelegramUserId() != userId` before deleting.

---

## 5. Bot Flow

### Happy Path

```
User sends: /remove
  → Bot: "Select a friend to remove." + paginated friend list keyboard [message key: select_remove]

User taps: "Alice (15.03.1990)"
  → State: WAITING_FOR_REMOVE_CONFIRM
  → Bot: "Remove Alice? This cannot be undone." + [✅ Yes] [❌ No] [message key: confirm_remove_ask]

User taps: "✅ Yes"
  → Friend deleted from DB
  → State: IDLE
  → Bot: "✅ Alice removed!" [message key: friend_removed]
```

### Cancel Path

```
User taps: "❌ No"
  → No deletion
  → State: IDLE
  → Bot: "❌ Removal cancelled. Your friend list is unchanged." [message key: confirm_remove_cancel]
```

### Empty List

```
User sends: /remove with no friends
  → Bot: "👥 No friends yet." + [➕ Add a friend] button [message key: friends_empty]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Bot Response (message key) |
|---|---|---|
| No friends | `/remove` with empty list | `friends_empty` + Add button |
| Friend deleted between list and tap | Friend ID not found in DB | `session_expired` |
| Cross-user deletion attempt | Friend's `telegramUserId` ≠ caller | `session_expired` |
| Paginate friend list | > 10 friends | ◀ / ▶ navigation buttons shown |

---

## 7. State Transitions

| From State | Event | To State |
|---|---|---|
| `IDLE` | `/remove` command | `IDLE` (list shown, no state change) |
| `IDLE` | Friend tapped (callback) | `WAITING_FOR_REMOVE_CONFIRM` |
| `WAITING_FOR_REMOVE_CONFIRM` | ✅ Yes tapped | `IDLE` |
| `WAITING_FOR_REMOVE_CONFIRM` | ❌ No tapped | `IDLE` |
| Any | `/cancel` | `IDLE` |

> States are defined in `BotState.java`

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Empty friend list | `friends_empty` | "👥 **No friends yet.** Use /add to add your first one." |
| Session expired / ownership fail | `session_expired` | "⏰ This action has expired. Please start again with the relevant command." |
| Confirm prompt | `confirm_remove_ask` | "Remove **%s**? This cannot be undone." |
| Removed successfully | `friend_removed` | "✅ **%s** removed! Use /add to add a new friend or /list to view your friends." |
| Removal cancelled | `confirm_remove_cancel` | "❌ Removal cancelled. Your friend list is unchanged. Use /list to view friends or /remove to try again." |

---

## 9. Acceptance Criteria

- [ ] Given the user has friends, when `/remove` is sent, then bot shows a paginated friend list.
- [ ] Given the user has no friends, when `/remove` is sent, then bot shows `friends_empty` with an Add button.
- [ ] Given the user taps a friend, then bot shows `confirm_remove_ask` with Yes/No buttons.
- [ ] Given the user taps Yes, then the friend is deleted and bot shows `friend_removed`.
- [ ] Given the user taps No, then no deletion occurs and bot shows `confirm_remove_cancel`.
- [ ] Given the friend was deleted between list display and confirmation, then bot shows `session_expired`.
- [ ] Given a crafted callback with another user's friend ID, then bot shows `session_expired` (ownership check).
- [ ] Given more than 10 friends, then pagination buttons are shown.
- [ ] Given `/cancel` is sent at any step, then state is cleared.
- [ ] Given the user's language is RU, then all messages are in Russian.

---

## 10. Data Model

Deletes from the `friends` collection. No writes to other collections.

| Operation | Collection | Condition |
|---|---|---|
| Delete by name | `friends` | `telegramUserId` + `name` (case-insensitive) |

> Lookup is by `id` (`_id`); deletion is by `name` via `deleteFriend(userId, name)`. The name is retrieved from the found friend document.

---

## 11. Security & Privacy

- **Ownership enforced:** `handleConfirmRemove` checks `friend.getTelegramUserId() != userId` before deleting — cross-user deletion is blocked.
- **Deletion:** Friend record is permanently removed. Also removed as part of `/deleteaccount`.
- **Exposure:** MongoDB `id` appears in callback data (`REMOVE_<id>`, `CONFIRM_REMOVE_<id>`) but is not shown in any user-facing message text.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `callback.friend.removed` | `DEBUG` | `userId`, `name` |

---

## 13. Known Limitations

- **Deletion is by name, not by ID** — `FriendService.deleteFriend()` uses `deleteByTelegramUserIdAndNameIgnoreCase`. The ID is used to look up the friend, but the actual delete query uses the name. If two friends somehow had the same name (data inconsistency), both could be deleted.
- **Pagination page size (10) is hardcoded** in `RemoveCommandHandler.PAGE_SIZE`.
- **No undo** — deletion is immediate and permanent with no recovery path.

---

## 14. Relationships to Other Features

- Affects: `/list`, `/today`, `/upcomingbirthdays`, `/jubilee`, `/stats`, `/export`, birthday reminders
- Empty state links to: `add-friend`
- All friends removed by: `delete-account`

---

## 15. Out of Scope

- Bulk removal of multiple friends at once
- Soft delete / archive
- Undo after deletion

---

## 16. Open Questions

- [ ] Should `deleteFriend` use ID-based deletion instead of name-based to avoid the theoretical double-delete on name collision?
- [ ] Should the confirmation message show the friend's birthdate as well, to help the user verify they selected the right person?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `RemoveCommandHandlerTest.java` | Friend list shown, empty list, `/cancel` hint, RU language |

**Not covered by tests:**
- Friend selection callback (`handleRemove`)
- Confirmation flow (`handleConfirmRemove`) — Yes and No paths
- Ownership check (cross-user deletion attempt)
- Session expiry between list and confirmation
- Pagination (> 10 friends)

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
