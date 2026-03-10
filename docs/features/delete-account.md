# Feature: Delete Account

> **Status:** `Stable`
> **Command:** `/deleteaccount`
> **Handler:** `DeleteAccountCommandHandler.java`, `CallbackQueryHandler.java` (`handleConfirmDeleteAccount`)

---

## 1. Overview

Allows a user to permanently delete all their data — friends list and preferences — from Festiva.
Requires explicit confirmation before deletion. Irreversible.

---

## 2. User Stories

- As a user, I want to delete all my data so that Festiva holds no information about me.
- As a user, I want a confirmation step so that I don't accidentally delete everything.

---

## 3. Functional Requirements

1. `/deleteaccount` shows a confirmation prompt with ✅ Yes / ❌ No buttons.
2. On confirm: all friends, user preferences, and in-memory state are permanently deleted.
3. On cancel: no data is deleted; bot confirms cancellation.
4. After deletion, user can start fresh with `/start`.
5. Deletion covers: all `friends` documents for the user, the `user_preferences` document, and in-memory `UserStateService` state.

---

## 4. Non-Functional Requirements

- **i18n:** All messages in both `messages_en.properties` and `messages_ru.properties`.
- **Irreversibility:** No soft-delete, no recovery path.
- **Atomicity:** Deletion is three sequential operations (`deleteAllFriends`, `deleteById`, `clearState`) — not wrapped in a transaction. Partial deletion is possible if one step fails.

---

## 5. Bot Flow

### Happy Path

```
User sends: /deleteaccount
  → Bot: "⚠️ Delete your account? This will permanently remove all your friends and preferences. This cannot be undone."
        + [✅ Yes] [❌ No] [message key: delete_account_ask]

User taps: "✅ Yes"
  → friendService.deleteAllFriends(userId)
  → userPreferenceRepository.deleteById(userId)
  → userStateService.clearState(userId)
  → Bot: "✅ Your account and all data have been deleted. Use /start to begin fresh." [message key: delete_account_done]
```

### Cancel Path

```
User taps: "❌ No"
  → No deletion
  → Bot: "❌ Deletion cancelled. Your data is safe." [message key: delete_account_cancel]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| User has no friends | Confirm on empty account | `deleteAllFriends` is a no-op; preferences still deleted |
| User has no preferences | Confirm with no prefs record | `deleteById` is a no-op; friends still deleted |
| Partial failure | One of the three delete steps throws | Remaining steps not executed; data partially deleted |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes during the confirmation flow.
`clearState` is called as part of the deletion itself.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Confirmation prompt | `delete_account_ask` | "⚠️ **Delete your account?** This will permanently remove all your friends and preferences. This cannot be undone." |
| Deleted successfully | `delete_account_done` | "✅ Your account and all data have been deleted. Use /start to begin fresh." |
| Cancelled | `delete_account_cancel` | "❌ Deletion cancelled. Your data is safe. Use /settings to adjust preferences or /help for commands." |

---

## 9. Acceptance Criteria

- [ ] Given `/deleteaccount` is sent, then bot shows `delete_account_ask` with Yes/No buttons.
- [ ] Given the user taps Yes, then all friends are deleted.
- [ ] Given the user taps Yes, then user preferences are deleted.
- [ ] Given the user taps Yes, then in-memory state is cleared.
- [ ] Given the user taps Yes, then bot shows `delete_account_done`.
- [ ] Given the user taps No, then no data is deleted and bot shows `delete_account_cancel`.
- [ ] Given the user has no friends, when Yes is tapped, then no exception is thrown.
- [ ] Given the user's language is RU, then all messages are in Russian.

---

## 10. Data Model

Deletes from two collections.

| Operation | Collection | Condition |
|---|---|---|
| Delete all friends | `friends` | `telegramUserId = userId` |
| Delete preferences | `user_preferences` | `_id = userId` |

In-memory state in `UserStateService` is also cleared (not persisted).

---

## 11. Security & Privacy

- **Ownership:** `deleteAllFriends` and `deleteById` are scoped to `telegramUserId` — only the requesting user's data is deleted.
- **Completeness:** All three data stores (friends, preferences, in-memory state) are cleared. No orphaned data remains after successful deletion.
- **GDPR relevance:** This is the primary data erasure mechanism. Partial failure (see Known Limitations) could leave data behind.
- **Exposure:** `userId` logged at `INFO` on deletion — acceptable for audit purposes.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `account.deleted` | `INFO` | `userId` |

---

## 13. Known Limitations

- **No transaction** — the three delete operations (`deleteAllFriends`, `deleteById`, `clearState`) are sequential with no rollback. If `deleteAllFriends` succeeds but `deleteById` throws, friends are gone but preferences remain.
- **In-memory state only** — `UserStateService.clearState` clears the in-memory cache. If the bot restarts before the cache is cleared, the state is already gone (stateless on restart), so this is low risk.
- **No confirmation of what will be deleted** — the prompt does not tell the user how many friends will be removed.

---

## 14. Relationships to Other Features

- Deletes data created by: `add-friend`, `bulk-add`, `edit-friend`, `settings`, `language`
- After deletion, user restarts with: `/start`

---

## 15. Out of Scope

- Partial deletion (e.g. delete only friends, keep preferences)
- Data export before deletion (user must run `/export` manually first)
- Account deactivation without data deletion

---

## 16. Open Questions

- [ ] Should the confirmation prompt show the number of friends that will be deleted?
- [ ] Should deletion be wrapped in a compensating transaction or at minimum reordered so the most critical delete (friends) is last, allowing retry?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `DeleteAccountCommandHandlerTest.java` | Confirmation prompt shown, Yes/No buttons present, `deleteAccount()` calls all three deletions, RU language |

**Not covered by tests:**
- Confirm callback end-to-end (`handleConfirmDeleteAccount` in `CallbackQueryHandler`)
- Cancel callback response (`delete_account_cancel`)
- Partial failure scenario (one delete step throws)
- User with no friends or no preferences

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
