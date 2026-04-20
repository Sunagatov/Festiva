# Festiva domain and state invariants

## Core domain object: Friend

A `Friend` is user-scoped birthday data.

Important fields:

- `telegramUserId`
- `name`
- `normalizedName`
- `birthYear` (nullable)
- `birthMonth`
- `birthDay`
- `notifyEnabled`
- `relationship`

## Friend invariants

- name cannot be blank
- name max length is 100
- duplicate normalized names are not allowed per user
- birthdays may be:
  - full date with year
  - month/day only, year unknown
- invalid dates must be rejected
- leap-day birthdays require careful next-birthday logic

## What unknown year means

If `birthYear == null`, the bot still tracks the birthday by month/day.

Consequences:

- age cannot be calculated
- “next age” cannot be shown
- reminder and month browsing still work

Do not silently convert unknown-year birthdays into full dates.

## Persistence-related rules

Friend uniqueness is effectively based on:

- `telegramUserId`
- normalized friend name

When changing name-related behavior, preserve:
- trimming
- normalization
- duplicate checks
- duplicate-key error handling

## User preferences

`UserPreference` stores long-lived per-user settings:

- language
- notify hour
- timezone
- last notified date

These settings affect reminders and language behavior.

## Conversational / session state

`BotState` includes:

- `IDLE`
- `WAITING_FOR_ADD_FRIEND_NAME`
- `WAITING_FOR_ADD_FRIEND_DATE`
- `WAITING_FOR_ADD_FRIEND_RELATIONSHIP`
- `WAITING_FOR_REMOVE_CONFIRM`
- `WAITING_FOR_EDIT_NAME`
- `WAITING_FOR_EDIT_DATE`
- `WAITING_FOR_SEARCH`
- `WAITING_FOR_BULK_ADD`
- `WAITING_FOR_EDIT_RELATIONSHIP`
- `WAITING_FOR_ICS_FILE`
- `WAITING_FOR_ICS_CONFIRM`

## UserStateService rules

`UserStateService` mixes:
- short-lived session state
- persistent preferences
- pending ICS import data

Be careful with:

- clearing state after success/cancel/error
- keeping language and settings persistent
- expiring stale sessions
- deleting pending ICS import data during state reset

## Reminder invariants

The reminder scheduler checks every hour in UTC, then decides per user whether to send.

Decision inputs:

- user timezone
- user notify hour
- last notified date
- `friend.isNotifyEnabled()`
- next birthday relative to the user's local date

Do not compare reminder dates using server-local assumptions.

## Callback safety rules

Inline callback data is user input in compact string form.

Always preserve:

- parse validation
- invalid-value handling
- session-expired behavior
- bounds checks for pages and hours
- timezone validation
- ownership checks for mutable actions
