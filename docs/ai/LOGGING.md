# Festiva logging guide

Use logs for production investigation, not code narration.

## Current policy

- `INFO`: meaningful production receipts only.
- `WARN`: abnormal but recoverable or suspicious conditions.
- `ERROR`: final or unexpected failures that need investigation.
- `DEBUG`: malformed client input, callback parsing noise, and local diagnosis detail.

## Good `INFO` events in this repo

- `telegram_bot_started`
- `birthday_reminder_check_completed`
- `bulk_add_completed`
- `ics_import_completed`

These are searchable receipts for startup, reminder runs, and write/import outcomes.

## Good `WARN` events in this repo

- `telegram_callback_ack_failed`
- `birthday_reminder_timezone_invalid`
- `bulk_add_file_download_failed`
- `ics_import_file_download_failed`
- `bot_commands_register_failed`
- `metrics_kafka_send_failed`

These indicate degraded behavior, retries/fallback territory, or operational follow-up without full request failure.

## Good `ERROR` events in this repo

- `telegram_bot_start_failed`
- `telegram_update_processing_failed`
- `telegram_update_fallback_message_failed`
- `telegram_notification_send_failed`
- `birthday_reminder_notification_failed`
- `friend_export_failed`

These are boundary failures or final external side-effect failures.

## Debug-only areas

Malformed callback input and stale inline-button paths are intentionally `DEBUG` in:

- `bot/CallbackQueryHandler.java`
- `bot/DatePickerCallbackHandler.java`

They are usually user/client noise, not incidents.

The main exception is `callback_unknown`, which remains `WARN` because it may indicate callback-contract drift or unexpected client behavior.

## What must not be logged

- raw Telegram message text
- friend names in backend logs unless absolutely required
- ICS summaries
- bot token, API keys, secrets, auth headers, cookies
- large DTO/entity dumps

Prefer safe identifiers and counts:

- `userId`
- `friendId`
- `updateId`
- `chatId`
- `durationMs`
- `errorCount`
- `savedCount`

## Config notes

- Production/runtime console formatting lives in `src/main/resources/application.yml`.
- Test noise suppression lives in `src/test/resources/application-test.yml` and `src/test/resources/logback-test.xml`.
- Test config keeps `com.festiva` visible at `INFO` while reducing framework startup chatter.

## Verification

Useful commands:

```bash
mvn test
mvn -Dtest=CallbackQueryHandlerTest,DatePickerCallbackHandlerTest test
mvn -Dtest=BirthdayReminderTest,BulkAddCommandHandlerTest,ImportIcsCommandHandlerTest test
```
