# Festiva — Logging Inventory

Style: `"domain.action: key=value"` — namespaced keys, structured key=value pairs, no prose sentences.

---

## Current Logs

### BirthdayBot

| Log key | Level | Location | Status |
|---|---|---|---|
| `bot.started` | INFO | `start()` | ✅ |
| `bot.commands.register.failed: message={}` | ERROR | `registerCommands()` | ✅ |
| `bot.update.null` | WARN | `consume()` | ✅ |
| `bot.update.failed: updateId={}, message={}` | ERROR | `consume()` | ✅ |
| `bot.notification.failed: userId={}, message={}` | ERROR | `send()` | ✅ |

**Missing:**
- `bot.commands.registered` INFO after `SetMyCommands` succeeds — confirms commands were registered on startup

---

### CallbackQueryHandler

| Log key | Level | Location | Status |
|---|---|---|---|
| `callback.unknown: data={}` | DEBUG | `handle()` | ✅ |
| `callback.language.unknown: code={}, reason={}` | WARN | `handleLanguage()` | ✅ |
| `callback.month.parse.failed: data={}, reason={}` | ERROR | `handleMonth()` | ⚠️ change to WARN — bad user input, not a system error |

**Missing:**
- `callback.language.changed: userId={}, lang={}` DEBUG — traces language switches
- `callback.friend.removed: userId={}, name={}` DEBUG — confirms removal via inline button

---

### CommandRouter

| Log key | Level | Location | Status |
|---|---|---|---|
| *(none)* | — | — | ⚠️ no logs at all |

**Missing:**
- `router.command: userId={}, command={}` DEBUG — essential for tracing which command was dispatched

---

### AddFriendCommandHandler

| Log key | Level | Location | Status |
|---|---|---|---|
| *(none)* | — | — | ⚠️ no logs at all |

**Missing:**
- `friend.added: userId={}, name={}` DEBUG — confirms successful add
- `friend.add.date.invalid: userId={}, input={}` DEBUG — date parse failure (user input, not system error)

---

### RemoveCommandHandler

| Log key | Level | Location | Status |
|---|---|---|---|
| *(none)* | — | — | ⚠️ no logs at all |

**Missing:**
- `friend.remove.not_found: userId={}, name={}` DEBUG — user tried to remove non-existent friend

---

### BirthdayReminder

| Log key | Level | Location | Status |
|---|---|---|---|
| `reminder.check.start` | INFO | `checkBirthdays()` | ✅ |
| `reminder.check.done: userCount={}` | INFO | `checkBirthdays()` | ⚠️ add `notifiedCount` — no way to know how many notifications were actually sent |
| `reminder.notify.failed: userId={}, friend={}, message={}` | ERROR | `checkAndNotify()` | ✅ |

**Missing:**
- `reminder.notify.sent: userId={}, friend={}, daysUntil={}` DEBUG — confirms each notification dispatched

---

### FestivaMetricsSender

| Log key | Level | Location | Status |
|---|---|---|---|
| `metrics.kafka.initialized: topic={}` | INFO | constructor | ✅ |
| `metrics.kafka.send.failed: message={}` | ERROR | send callback | ✅ |
| `metrics.payload.build.failed: message={}` | ERROR | `sendMetrics()` | ✅ |
| `metrics.serialize.failed: message={}` | ERROR | `buildJson()` | ✅ |
| `metrics.kafka.closed` | INFO | `close()` | ✅ |

---

## Action Plan

| # | Action | File | Type |
|---|---|---|---|
| 1 | Add `bot.commands.registered` INFO | `BirthdayBot.java` | ✅ done |
| 2 | Change `callback.month.parse.failed` to WARN | `CallbackQueryHandler.java` | ✅ done |
| 3 | Add `callback.language.changed: userId={}, lang={}` DEBUG | `CallbackQueryHandler.java` | ✅ done |
| 4 | Add `callback.friend.removed: userId={}, name={}` DEBUG | `CallbackQueryHandler.java` | ✅ done |
| 5 | Add `router.command: userId={}, command={}` DEBUG | `CommandRouter.java` | ✅ done |
| 6 | Add `friend.added: userId={}, name={}` DEBUG | `AddFriendCommandHandler.java` | ✅ done |
| 7 | Add `friend.add.date.invalid: userId={}, input={}` DEBUG | `AddFriendCommandHandler.java` | ✅ done |
| 8 | Add `friend.remove.not_found: userId={}, name={}` DEBUG | `RemoveCommandHandler.java` | ✅ done |
| 9 | Add `notifiedCount` to `reminder.check.done` | `BirthdayReminder.java` | ✅ done |
| 10 | Add `reminder.notify.sent: userId={}, friend={}, daysUntil={}` DEBUG | `BirthdayReminder.java` | ✅ done |
| 11 | Add `date={}` to `reminder.check.start` | `BirthdayReminder.java` | ✅ done |
| 12 | Add `router.state: userId={}, state={}` DEBUG for stateful routing | `CommandRouter.java` | ✅ done |
| 13 | Add `router.command` DEBUG for `/cancel` path | `CommandRouter.java` | ✅ done |
| 14 | Add `type={}` to `bot.update.failed` | `BirthdayBot.java` | ✅ done |
| 15 | Add `friend.removed` DEBUG on text-input removal success | `RemoveCommandHandler.java` | ✅ done |
| 16 | Add `router.command.unknown: userId={}, text={}` DEBUG | `DefaultCommandHandler.java` | ✅ done |
| 17 | Add `session.cancelled: userId={}` DEBUG | `CancelCommandHandler.java` | ✅ done |
| 18 | Demote `org.telegram.telegrambots` from DEBUG to INFO | `application.yml` | ✅ done |
| 19 | Add `friend.add.session.lost: userId={}` WARN for null pending name | `AddFriendCommandHandler.java` | ✅ done |
