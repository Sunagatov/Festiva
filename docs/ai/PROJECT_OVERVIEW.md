# Festiva project overview

## What Festiva is

Festiva is a Telegram birthday reminder bot.

Core capabilities:

- add, edit, remove, search, list, and export friends
- browse birthdays by month
- show today / upcoming / jubilee birthdays
- schedule reminders
- manage settings such as language, timezone, and reminder hour
- bulk import via CSV / paste
- import birthday data from `.ics`
- optionally use AI to extract a cleaner name from ICS event titles

## Runtime model

- app starts as a Spring Boot application
- bot uses Telegram long polling
- update handling begins in `BirthdayBot`
- text and document messages are routed through `CommandRouter`
- inline button clicks are routed through `CallbackQueryHandler`
- user conversational state is persisted via `UserStateService`
- reminders are scheduled hourly in UTC, then filtered by user timezone + notify hour

## Main technology choices

- Java 25
- Spring Boot 4
- MongoDB
- Telegram Bots Java library
- optional Kafka metrics
- optional LangChain4j / OpenAI for ICS name extraction
- Caffeine cache for session access

## Configuration surface

Main runtime config lives in `src/main/resources/application.yml`.

Key knobs:

- Mongo URI and DB name
- Telegram bot token and username
- Kafka enablement + bootstrap/credentials
- AI enablement + base URL + model name
- logging policy and test log-noise controls under `docs/ai/LOGGING.md`

## Code shape

Main package root: `src/main/java/com/festiva`

Major areas:

- `bot/`
- `command/`
- `friend/`
- `i18n/`
- `metrics/`
- `notification/`
- `state/`
- `user/`
- `ai/`

## Design consequences for code agents

This is not a generic CRUD app.

Changes often cross multiple areas at once:

- handler logic
- callback contract
- user state
- i18n text
- persistence rules
- reminder behavior
- tests

That is why targeted impact mapping matters more than repo-wide searching.
