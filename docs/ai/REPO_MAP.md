# Festiva repo map

Use this map to choose the **smallest useful read-set**.

## First-stop files

### Application / config
- `src/main/java/com/festiva/FestivaApplication.java`
- `src/main/resources/application.yml`
- `pom.xml`

### Update entrypoints
- `src/main/java/com/festiva/bot/BirthdayBot.java`
- `src/main/java/com/festiva/command/CommandRouter.java`
- `src/main/java/com/festiva/bot/CallbackQueryHandler.java`

## Domain and persistence

### Friend model and rules
- `src/main/java/com/festiva/friend/entity/Friend.java`
- `src/main/java/com/festiva/friend/entity/Relationship.java`
- `src/main/java/com/festiva/friend/api/FriendService.java`
- `src/main/java/com/festiva/friend/repository/*`

### User preferences and session state
- `src/main/java/com/festiva/user/UserPreference.java`
- `src/main/java/com/festiva/user/UserPreferenceRepository.java`
- `src/main/java/com/festiva/state/BotState.java`
- `src/main/java/com/festiva/state/UserStateService.java`
- `src/main/java/com/festiva/state/UserSession*`
- `src/main/java/com/festiva/state/PendingImport*`

## Feature hotspots

### Add / edit / remove / search / list
- `src/main/java/com/festiva/command/handler/*Add*`
- `src/main/java/com/festiva/command/handler/*Edit*`
- `src/main/java/com/festiva/command/handler/*Remove*`
- `src/main/java/com/festiva/command/handler/*Search*`
- `src/main/java/com/festiva/command/handler/*List*`
- `src/main/java/com/festiva/bot/EditCallbackHandler.java`
- `src/main/java/com/festiva/bot/DatePickerCallbackHandler.java`
- `src/main/java/com/festiva/command/DatePickerKeyboard.java`

### Settings / language
- `src/main/java/com/festiva/command/handler/SettingsCommandHandler.java`
- `src/main/java/com/festiva/bot/BotCommandsService.java`
- `src/main/java/com/festiva/state/UserStateService.java`
- `src/main/java/com/festiva/i18n/*`

### Reminders
- `src/main/java/com/festiva/notification/BirthdayReminder.java`
- `src/main/java/com/festiva/notification/NotificationSender.java`
- `src/main/java/com/festiva/user/UserPreference.java`
- `src/main/java/com/festiva/friend/api/FriendService.java`

### ICS import / AI extraction
- `src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java`
- `src/main/java/com/festiva/ai/*`
- `src/main/java/com/festiva/state/UserStateService.java`

### Metrics
- `src/main/java/com/festiva/metrics/*`

## i18n

Any user-visible change may require checking:

- `src/main/java/com/festiva/i18n/Lang.java`
- `src/main/java/com/festiva/i18n/Messages.java`

## Tests

Check `src/test/java/com/festiva/**` for:
- targeted handler tests
- service tests
- reminder tests
- integration tests using Testcontainers

## Read-late, not read-first

Avoid these until the task clearly needs them:

- full feature docs under `docs/features/`
- deployment files
- observability extras
- unrelated command handlers
- generated build output
