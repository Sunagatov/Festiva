# Abuse protection

Festiva admits Telegram messages and callbacks before acknowledgements, routing,
database access or metrics. A single ordered worker has a queue of 32 updates;
overload is dropped without sending another reply. Reminders run separately.
Rejected updates are counted in one log summary per minute.

| Setting | Default |
| --- | --- |
| `FESTIVA_ABUSE_UPDATES_PER_USER_MINUTE` | 30 |
| `FESTIVA_ABUSE_UPDATES_PER_MINUTE` | 300 across users |
| `FESTIVA_ABUSE_IMPORTS_PER_USER_HOUR` | 3 |
| `FESTIVA_ABUSE_IMPORTS_PER_HOUR` | 20 across users |
| `AI_DAILY_CALL_LIMIT` | 100 across users, resets at midnight UTC |

Update and import counters are process-local fixed windows. Already-throttled
users cannot consume other users' shared allowance. Admitted import attempts,
including bad files, consume allowance; changing commands or cancelling does not
reset it. Denial preserves the current friend list and pending conversation state.

Downloads enforce 512 KiB of actual bytes, 10,000 physical lines and 4,096
characters per line. CSV retains its existing 50-entry limit and quoted multiline
support. ICS additionally caps unfolded lines at 4,096 characters, the calendar
at 1,000 events and the birthday selection at 100 entries. Oversized calendars
are rejected before name extraction or persistence; split them into smaller files.
Downloads have a three-second connection/read-idle timeout and a ten-second
elapsed read deadline checked between reads. A final blocking read may add one
idle interval. Telegram API operations also have a ten-second call timeout.

AI remains optional and its existing provider/key configuration is preserved.
One atomic Mongo document in `festiva_ai_usage` reserves the daily allowance
before each provider call, including failed calls. Restarts, retries and different
users cannot refill it. Database failure or exhaustion skips paid work and uses
the calendar title. Set `AI_DAILY_CALL_LIMIT=0` to pause AI extraction.

AI receives at most 200 input characters, requests at most 64 completion tokens,
uses a two-second request timeout, and has no automatic retries. Each import
attempts at most ten AI extractions, starting none after its five-second enrichment
window; remaining entries keep their original titles. Full friend lists skip AI.

`mvn test` verifies admission, queue overload, import limits and normal EN/RU flows.
AI budget integration tests use disposable MongoDB; provider tests are mocked.
Keep `AI_ENABLED=false` when running tests unless explicitly running the existing
opt-in live-provider tests. Production container and log bounds belong in Vault.
