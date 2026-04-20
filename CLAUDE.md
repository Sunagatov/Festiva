# Festiva — Agent Instructions

## Mission

Work as a **targeted code assistant** for Festiva, not a broad repo summarizer.

Your priorities:

1. read the minimum number of files needed
2. keep changes local and consistent
3. preserve Telegram bot flows, user state, i18n, and reminder semantics
4. verify with focused tests before suggesting broad refactors

## Fast reading order

For most tasks, read in this order:

1. `README.md`
2. `docs/ai/PROJECT_OVERVIEW.md`
3. `docs/ai/REPO_MAP.md`
4. `docs/ai/CHANGE_IMPACT_GUIDE.md`
5. only then the smallest set of relevant source files

Do **not** start with broad repo-wide scans unless the task is explicitly architectural.

## Repo facts you should assume

- Java 25, Spring Boot 4, Maven
- Telegram long-polling bot
- MongoDB for persistence
- optional Kafka metrics
- optional AI-assisted ICS name extraction
- virtual threads are enabled
- reminder scheduler runs hourly in UTC and applies user timezone + user notify hour
- bot supports both English and Russian
- `Friend` may have known year **or** unknown year
- duplicate friend names are prevented per user by normalized name

## High-signal hotspots

- Telegram update entry point: `src/main/java/com/festiva/bot/BirthdayBot.java`
- text/document routing: `src/main/java/com/festiva/command/CommandRouter.java`
- inline callback routing: `src/main/java/com/festiva/bot/CallbackQueryHandler.java`
- domain validation and persistence logic: `src/main/java/com/festiva/friend/api/FriendService.java`
- friend invariants: `src/main/java/com/festiva/friend/entity/Friend.java`
- session and preferences: `src/main/java/com/festiva/state/UserStateService.java`, `src/main/java/com/festiva/user/UserPreference.java`
- reminders: `src/main/java/com/festiva/notification/BirthdayReminder.java`
- ICS import + optional AI extraction: `src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java`, `src/main/java/com/festiva/ai/IcsNameExtractorService.java`
- config: `src/main/resources/application.yml`

## Hard rules

### 1) Do not break i18n
If a flow changes user-visible text, check both EN and RU messages.

### 2) Preserve callback contracts
If inline keyboard data changes, update callback handling and keep parse/error paths safe.

### 3) Preserve ownership checks
Any edit/remove/toggle action must remain scoped to the current Telegram user.

### 4) Preserve unknown-year birthdays
Do not force a birth year where the feature intentionally supports month/day only.

### 5) Preserve reminder semantics
Reminder logic depends on:
- timezone
- notify hour
- last notified date
- next birthday calculation
- leap day behavior

### 6) Keep logs useful
Prefer structured, concise logs around failures and rejected input.
Avoid noisy debug-style logs unless explicitly requested.

### 7) Avoid broad scans
Do not read:
- `target/`
- large generated outputs
- docs unrelated to the active task
- every handler in the repo "just in case"

## How to approach common tasks

### Bug fix
- identify exact failing flow
- read entry point + target hotspot + direct dependencies
- form a root-cause hypothesis early
- check linked invariants in `docs/ai/DOMAIN_AND_STATE.md`
- verify with a focused regression test

### New feature
- map the flow first using `docs/ai/CHANGE_IMPACT_GUIDE.md`
- keep the first patch narrow
- wire command/callback/state/i18n/tests together in the same change
- avoid “cleanup refactors” inside feature PRs unless necessary

### Review
- start from changed files only
- then check immediate impact neighbors
- look for broken i18n, callback parsing, state clearing, timezone behavior, ownership validation, duplicate-name handling, and missing tests

## Default verification

Use the smallest verification that gives confidence:

- targeted unit test
- related test class
- `mvn test` for broader confidence
- Docker-backed flows only when needed

See `docs/ai/VERIFICATION.md`.
