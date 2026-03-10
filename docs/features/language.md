# Feature: Language

> **Status:** `Stable`
> **Command:** `/language`
> **Handler:** `LanguageCommandHandler.java`, `CallbackQueryHandler.java` (`handleLanguage`)

---

## 1. Overview

Allows a user to switch the bot's language between English and Russian.
The selection is persisted and applied immediately to all subsequent messages.

---

## 2. User Stories

- As a user, I want to switch the bot language to my preferred language so that I can use Festiva comfortably.

---

## 3. Functional Requirements

1. `/language` shows a two-button keyboard: 🇬🇧 English and 🇷🇺 Russian.
2. The currently active language is marked with ✅.
3. Tapping a language saves it immediately and refreshes the keyboard in-place.
4. The confirmation message is shown in the **newly selected** language.
5. All subsequent bot messages use the newly selected language.
6. If an unknown language code arrives in the callback, bot replies with `session_expired`.

---

## 4. Non-Functional Requirements

- **i18n:** Button labels and prompt are rendered in the user's **current** language. Confirmation is rendered in the **new** language.
- **Persistence:** Language saved to `user_preferences` via `UserStateService.setLanguage()`.
- **Supported languages:** `EN` and `RU` only — defined by the `Lang` enum.

---

## 5. Bot Flow

### Happy Path

```
User sends: /language
  → Bot reads current language
  → Bot: "🌐 Choose your language:" + [✅ 🇬🇧 English] [🇷🇺 Russian] [message key: language_choose]

User taps: "🇷🇺 Russian"
  → Language saved as RU
  → Keyboard refreshed: [🇬🇧 English] [✅ 🇷🇺 Русский]
  → Bot: "✅ Язык установлен: Русский 🇷🇺." [message key: language_set, in new language]
```

---

## 6. Edge Cases & Alternative Flows

| Scenario | Trigger | Behaviour |
|---|---|---|
| User taps already-active language | Taps ✅ English again | Saved again (no-op); confirmation shown in same language |
| Unknown language code in callback | Malformed `LANG_` callback | `session_expired` shown in current language |

---

## 7. State Transitions

This feature is stateless — no `BotState` changes occur.

---

## 8. Error Messages

| Scenario | Message Key | EN Text |
|---|---|---|
| Prompt | `language_choose` | "🌐 **Choose your language:**" |
| Language set | `language_set` | "✅ Language set to **English** 🇬🇧. Use /language to change it again." |
| Unknown code | `session_expired` | "⏰ This action has expired. Please start again with the relevant command." |

---

## 9. Acceptance Criteria

- [ ] Given `/language` is sent, then bot shows a keyboard with EN and RU buttons.
- [ ] Given the current language is EN, then the EN button has ✅ and RU does not.
- [ ] Given the current language is RU, then the RU button has ✅ and EN does not.
- [ ] Given the user taps RU, then language is saved as RU and confirmation is shown in Russian.
- [ ] Given the user taps EN, then language is saved as EN and confirmation is shown in English.
- [ ] Given the user taps the already-active language, then it is saved again and confirmation is shown.
- [ ] Given an unknown language code arrives, then `session_expired` is shown in the current language.

---

## 10. Data Model

Reads and writes `user_preferences` via `UserStateService`.

| Field | Type | Notes |
|---|---|---|
| `lang` | `Lang` | `EN` or `RU`; persisted in `user_preferences` |

---

## 11. Security & Privacy

- **Ownership:** Language preference is scoped to `telegramUserId`.
- **Deletion:** `user_preferences` document removed on `/deleteaccount`.
- **Exposure:** No sensitive fields exposed.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `callback.language.changed` | `DEBUG` | `userId`, `lang` |
| `callback.language.unknown` | `WARN` | `code` |

---

## 13. Known Limitations

- **Only EN and RU supported** — adding a new language requires code changes to `Lang` enum, message property files, and `Relationship` enum labels.
- **No auto-detection** — language is not inferred from the user's Telegram locale; it must be set manually.
- **Default language is RU** — new users who never set a language receive Russian messages (default in `BirthdayReminder` and fallback in `UserStateService`).

---

## 14. Relationships to Other Features

- Affects: all features that display messages
- Preference stored alongside: `settings` (same `user_preferences` document)
- Cleared by: `delete-account`

---

## 15. Out of Scope

- Auto-detection from Telegram user locale
- Languages beyond EN and RU

---

## 16. Open Questions

- [ ] Should the default language be EN instead of RU given the project targets an international audience?
- [ ] Should language be auto-detected from the Telegram user's locale on first `/start`?

---

## 17. Testing Notes

| Test Class | What's Covered |
|---|---|
| `LanguageCommandHandlerTest.java` | Prompt shown, EN ✅ marker, RU ✅ marker |

**Not covered by tests:**
- Language save callback (`handleLanguage`) — value persisted, keyboard refreshed, confirmation in new language
- Unknown language code → `session_expired`
- Default language (RU) for new users

---

## 18. Changelog

| Date | Change |
|---|---|
| 2025-07-14 | Initial spec created |
