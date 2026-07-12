# Festiva Agent Guide

These instructions are for Claude, Codex, Amazon Q / Kiro, Copilot, ChatGPT,
and other AI agents working in this source repository.

## Mission

Work as a targeted code assistant for Festiva, not a broad repo summarizer.

Default behavior: read the smallest useful docs first, preserve Telegram bot
flow semantics, and verify with focused tests.

## Source-Of-Truth Boundary

- This repository owns Festiva application source code, local tests, and
  source-level docs.
- Vault owns production runtime files, deployment, secrets, observability,
  backups, and infra wiring when a local Vault checkout is available.
- Do not copy Vault secrets or deployment procedures into this repo's adapters.

## Vault Context

The sibling `Vault` checkout is the private operations and knowledge-base repo
shared across these pet projects. Use it only when a task needs
production/runtime facts: deployment flow, Docker Compose on the host, systemd or
host setup, SOPS-managed secrets, backups/restores, observability, reverse proxy,
infra inventory, or cross-project operational decisions. Start with Vault's
`AGENTS.md` and follow its routing docs instead of asking where production,
secrets, monitoring, or server-state information lives.

## Minimal Read Order

1. `AGENTS.md`
2. `docs/ai/PROJECT_OVERVIEW.md`
3. `docs/ai/REPO_MAP.md`
4. `docs/ai/CHANGE_IMPACT_GUIDE.md`
5. The smallest relevant source files or feature docs.

Use feature docs under `docs/features/` only when the task touches that flow.
Avoid `target/`, generated outputs, and unrelated handlers.

## Durable Repo Facts

- Java 25, Spring Boot 4, Maven.
- Telegram long-polling bot.
- MongoDB for persistence.
- Optional Kafka metrics.
- Optional AI-assisted ICS name extraction.
- Virtual threads are enabled.
- Bot supports English and Russian.
- `Friend` may have a known year or unknown year.
- Duplicate friend names are prevented per user by normalized name.
- Reminder scheduler runs hourly in UTC and applies user timezone, notify hour,
  and `lastNotifiedDate`.

## High-Signal Hotspots

- Telegram update entry point: `src/main/java/com/festiva/bot/BirthdayBot.java`
- Text/document routing: `src/main/java/com/festiva/command/CommandRouter.java`
- Inline callbacks: `src/main/java/com/festiva/bot/CallbackQueryHandler.java`
- Domain validation and persistence: `src/main/java/com/festiva/friend/api/FriendService.java`
- Friend invariants: `src/main/java/com/festiva/friend/entity/Friend.java`
- Session/preferences: `src/main/java/com/festiva/state/UserStateService.java`,
  `src/main/java/com/festiva/user/UserPreference.java`
- Reminders: `src/main/java/com/festiva/notification/BirthdayReminder.java`
- ICS import and AI extraction:
  `src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java`,
  `src/main/java/com/festiva/ai/IcsNameExtractorService.java`
- Config: `src/main/resources/application.yml`

## Hard Rules

1. Do not break EN/RU i18n. If user-visible text changes, check both languages.
2. Preserve Telegram callback data contracts and safe parse/error paths.
3. Preserve per-user ownership checks for edit/remove/toggle actions.
4. Preserve unknown-year birthdays; do not force a birth year.
5. Preserve reminder semantics: timezone, notify hour, last notified date, next
   birthday calculation, and leap-day behavior.
6. Keep logs structured and concise.
7. Do not broaden into unrelated docs, generated files, or every handler unless
   the task explicitly requires architecture-wide work.

## Common Task Routing

- Bug fix: identify the exact failing flow, read the entry point plus direct
  dependencies, check `docs/ai/DOMAIN_AND_STATE.md`, then add or run a focused
  regression test.
- New feature: map command/callback/state/i18n/test impact with
  `docs/ai/CHANGE_IMPACT_GUIDE.md`; keep the first patch narrow.
- Review: start from changed files, then immediate impact neighbors.

## Verification

Use the smallest verification that gives confidence:

```bash
mvn test
```

For narrower changes, run the related test class or targeted Maven test command.
See `docs/ai/VERIFICATION.md`.
