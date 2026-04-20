# Festiva change impact guide

Use this before editing code. It helps avoid one-file fixes that break a multi-file flow.

## 1) Telegram update handling

### Primary files
- `bot/BirthdayBot.java`
- `command/CommandRouter.java`
- `bot/CallbackQueryHandler.java`

### Also check
- `i18n/Messages.java`
- tests around routing / callbacks

### Typical risks
- swallowed exceptions
- wrong response type
- message-vs-callback divergence
- missing fallback error handling

---

## 2) Friend creation / edit / delete / toggle

### Primary files
- `friend/entity/Friend.java`
- `friend/api/FriendService.java`
- relevant handler(s) in `command/handler/`
- callback helper(s) in `bot/`

### Also check
- user session cleanup
- duplicate-name logic
- month/day vs full-date handling
- tests

### Typical risks
- duplicate names after normalization
- invalid date acceptance
- cross-user access
- stale pending state

---

## 3) Inline keyboard and callback behavior

### Primary files
- `bot/CallbackQueryHandler.java`
- related handler class
- `command/DatePickerKeyboard.java`

### Also check
- callback constant names
- parsing helpers
- invalid input fallback
- keyboard generation

### Typical risks
- broken callback prefixes
- parse failures
- missing session-expired response
- “message is not modified” handling

---

## 4) Reminder logic

### Primary files
- `notification/BirthdayReminder.java`
- `user/UserPreference.java`
- `state/UserStateService.java`
- `friend/entity/Friend.java`

### Also check
- message templates
- timezone behavior
- leap-day birthdays
- notify-enabled flag

### Typical risks
- duplicate reminders
- wrong local day
- invalid timezone fallback
- wrong age / next age text

---

## 5) ICS import

### Primary files
- `command/handler/ImportIcsCommandHandler.java`
- `state/UserStateService.java`
- `friend/entity/Friend.java`
- `friend/api/FriendService.java`

### Also check
- AI extractor under `ai/`
- file type / size validation
- duplicate-name handling
- state clear on confirm/cancel/error

### Typical risks
- trusting bad years
- duplicate imports
- broken pending import lifecycle
- unsafe fallback when AI extraction fails

---

## 6) Language / settings

### Primary files
- `command/handler/SettingsCommandHandler.java`
- `bot/CallbackQueryHandler.java`
- `state/UserStateService.java`
- `user/UserPreference.java`
- `bot/BotCommandsService.java`

### Also check
- both EN and RU text
- command registration
- timezone validation

### Typical risks
- settings update without persistence
- invalid timezone acceptance
- language change not reflected in commands

---

## 7) Metrics / observability

### Primary files
- `metrics/*`
- `bot/BirthdayBot.java`
- `src/main/resources/application.yml`

### Also check
- error paths
- optional Kafka enablement
- failure tolerance

### Typical risks
- metrics failures breaking bot flow
- noisy logging
- null-safe payload handling

---

## 8) User-visible text changes

### Primary files
- `i18n/Messages.java`
- relevant handler(s)

### Also check
- HTML escaping
- EN + RU coverage
- tests for exact formatting when needed

### Typical risks
- untranslated path
- broken placeholders
- inconsistent HTML formatting
