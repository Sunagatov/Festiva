# Festiva compact agent context

Generated on: 2026-04-20 20:42:28 UTC

Purpose: small, high-signal context for Claude/Cloudy-style repo work.


---

## FILE: CLAUDE.md

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


---

## FILE: docs/ai/PROJECT_OVERVIEW.md

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


---

## FILE: docs/ai/REPO_MAP.md

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


---

## FILE: docs/ai/DOMAIN_AND_STATE.md

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


---

## FILE: docs/ai/CHANGE_IMPACT_GUIDE.md

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


---

## FILE: docs/ai/TOKEN_EFFICIENCY.md

# Token efficiency rules for Festiva

These rules are specifically meant to make agent runs cheaper and faster.

## Core principle

Do not pay tokens to rediscover stable repo knowledge.

Use the context docs first, then read only the smallest set of source files needed.

## Reading policy

### Good
- read 3 to 6 targeted files first
- follow symbol references only when needed
- inspect direct neighbors of the changed file
- use repo map + impact guide to narrow scope

### Bad
- reading every handler for a small bug
- opening full feature docs before code
- scanning all tests before finding the touched flow
- repeating repo summaries in every run

## Default file budget by task

### Small bug
Target: 4 to 8 files

Suggested order:
1. entrypoint / router
2. target handler or service
3. one domain file
4. one test
5. one helper if required

### Medium feature
Target: 8 to 14 files

Suggested order:
1. repo map
2. impact guide
3. primary handler/service files
4. domain/state/i18n
5. direct tests

### Architecture review
Read wider only after building a hotspot list.

## Output policy

Keep outputs compact:

- root cause
- impacted files
- minimal patch plan
- regression tests
- verification command

Do not dump long file summaries unless explicitly requested.

## Search policy

Search by:
- class name
- method name
- callback prefix
- command name
- exact message key
- domain field name

Avoid fuzzy repo-wide exploration first.

## When to read docs/features

Read `docs/features/*` only when you need:
- acceptance criteria
- expected UX copy
- edge-case product behavior

Do not use feature docs as the first stop for implementation bugs.

## Best practical workflow

1. run `./scripts/ai/print-hotspots.sh <topic>`
2. read `CLAUDE.md`
3. read the 2–5 primary files
4. form a hypothesis
5. read only missing dependencies
6. patch
7. run the smallest useful verification


---

## FILE: docs/ai/VERIFICATION.md

# Festiva verification guide

Pick the smallest verification that gives confidence.

## Basic commands

### Run all tests
```bash
mvn test
```

### Run one test class
```bash
mvn -Dtest=ClassName test
```

### Run one test method
```bash
mvn -Dtest=ClassName#methodName test
```

## Local runtime

### Start Mongo only
```bash
docker compose up -d mongo
```

### Run the app from shell
```bash
set -a && source .env && set +a && mvn spring-boot:run
```

### Run the bot container profile
```bash
docker compose --profile bot up -d --build
```

## Regression checklist by change type

### Routing / command handling
- unknown text behavior
- command dispatch
- stateful message handling
- document handling if relevant

### Callback changes
- valid callback path
- invalid callback value
- parse failure path
- session-expired path

### Friend validation
- blank name rejected
- name too long rejected
- duplicate normalized name rejected
- invalid date rejected
- unknown-year birthday still accepted

### Reminder changes
- timezone handling
- notify hour handling
- lastNotifiedDate duplicate suppression
- leap day birthday behavior
- notifyEnabled false path

### ICS import changes
- wrong file type rejected
- oversized file rejected
- empty / invalid ICS rejected
- duplicate names filtered
- pending import confirm/cancel works
- AI failure falls back safely

### i18n changes
- EN path still works
- RU path still works
- placeholders render correctly
- HTML escaping remains correct

## What not to skip

Even for small fixes, do not skip tests when you touched:
- callback parsing
- date logic
- duplicate handling
- reminder behavior
- session clearing


---

## SNIPPET: src/main/resources/application.yml (first 200 lines)

spring:
  threads:
    virtual:
      enabled: true
  mongodb:
    uri: ${MONGO_URI:mongodb://localhost:27017}
    database: ${MONGO_DATABASE_NAME:FestivaDatabase}

management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    username: ${TELEGRAM_BOT_USERNAME}

logging:
  level:
    org.telegram.telegrambots: INFO
  group:
    festiva: com.festiva
    telegram: org.telegram.telegrambots

kafka:
  enabled: ${APP_KAFKA_ENABLED:false}
  topic: festiva.bot.metric
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:}
  api-key: ${KAFKA_API_KEY:}
  api-secret: ${KAFKA_API_SECRET:}

ai:
  enabled: ${AI_ENABLED:false}
  api-key: ${OPENAI_API_KEY:}
  base-url: ${AI_BASE_URL:https://api.openai.com/v1}
  model-name: ${AI_MODEL_NAME:gpt-4o-mini}


---

## SNIPPET: src/main/java/com/festiva/FestivaApplication.java (first 120 lines)

package com.festiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FestivaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestivaApplication.class, args);
    }
}


---

## SNIPPET: src/main/java/com/festiva/bot/BirthdayBot.java (first 240 lines)

package com.festiva.bot;

import com.festiva.command.CommandRouter;
import com.festiva.i18n.Lang;
import com.festiva.metrics.MetricsSender;
import com.festiva.notification.NotificationSender;
import com.festiva.state.UserStateService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class BirthdayBot implements LongPollingSingleThreadUpdateConsumer, NotificationSender {

    private final String botToken;
    private final TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;
    private final CommandRouter commandRouter;
    private final CallbackQueryHandler callbackQueryHandler;
    private final MetricsSender metricsSender;
    private final BotCommandsService commandsService;
    private final UserStateService userStateService;
    private final ExecutorService heavyWorkExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public BirthdayBot(CommandRouter commandRouter,
                       CallbackQueryHandler callbackQueryHandler,
                       TelegramClient telegramClient,
                       @Value("${telegram.bot.token}") String botToken,
                       MetricsSender metricsSender,
                       BotCommandsService commandsService,
                       UserStateService userStateService) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.commandRouter = commandRouter;
        this.callbackQueryHandler = callbackQueryHandler;
        this.metricsSender = metricsSender;
        this.commandsService = commandsService;
        this.userStateService = userStateService;
    }

    @PostConstruct
    public void start() {
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, this);
            log.info("bot.started");
        } catch (TelegramApiException e) {
            log.error("bot.start.failed", e);
            throw new RuntimeException("bot.start.failed", e);
        }
        commandsService.registerGlobalCommands();
    }

    @Override
    public void consume(Update update) {
        if (update == null) {
            log.warn("bot.update.null");
            return;
        }

        if (update.hasCallbackQuery()) {
            String callbackId = update.getCallbackQuery().getId();
            try {
                telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
            } catch (TelegramApiException e) {
                log.warn("bot.callback.answer.failed: callbackId={}", callbackId, e);
            }
        }

        heavyWorkExecutor.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {
        long startTime = System.currentTimeMillis();
        String updateType = update.hasCallbackQuery() ? "callback" : update.hasMessage() ? "message" : "other";
        try {
            if (update.hasCallbackQuery()) {
                EditMessageText edit = callbackQueryHandler.handle(update.getCallbackQuery());
                if (edit != null) {
                    try {
                        telegramClient.execute(edit);
                    } catch (TelegramApiException e) {
                        if (e.getMessage() == null || !e.getMessage().contains("message is not modified")) {
                            throw e;
                        }
                    }
                }
            } else if (update.hasMessage()) {
                SendMessage response = commandRouter.route(update);
                if (response != null) {
                    telegramClient.execute(response);
                }
            }
            metricsSender.sendMetrics(update, "SUCCESS", System.currentTimeMillis() - startTime);
        } catch (TelegramApiException | RuntimeException e) {
            metricsSender.sendMetrics(update, "ERROR", System.currentTimeMillis() - startTime);
            log.error("bot.update.failed: updateId={}, type={}, message={}", update.getUpdateId(), updateType, e.getMessage(), e);

            try {
                long chatId = update.hasCallbackQuery()
                        ? update.getCallbackQuery().getMessage().getChatId()
                        : update.hasMessage() ? update.getMessage().getChatId() : 0;
                long userId = update.hasCallbackQuery()
                        ? update.getCallbackQuery().getFrom().getId()
                        : update.hasMessage() ? update.getMessage().getFrom().getId() : 0;

                if (chatId > 0 && userId > 0) {
                    Lang lang = userStateService.getLanguage(userId);
                    String errorMsg = lang == Lang.RU
                            ? "⚠️ Произошла ошибка. Попробуйте снова или используйте /cancel"
                            : "⚠️ An error occurred. Please try again or use /cancel";
                    telegramClient.execute(SendMessage.builder().chatId(chatId).text(errorMsg).build());
                }
            } catch (Exception fallbackError) {
                log.error("bot.error.fallback.failed", fallbackError);
            }
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (botsApplication != null) {
            botsApplication.close();
        }
        heavyWorkExecutor.shutdown();
    }

    @Override
    public void send(long telegramUserId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(telegramUserId).parseMode("HTML").text(text).build());
        } catch (TelegramApiException e) {
            log.error("bot.notification.failed: userId={}, message={}", telegramUserId, e.getMessage(), e);
        }
    }
}

---

## SNIPPET: src/main/java/com/festiva/command/CommandRouter.java (first 220 lines)

package com.festiva.command;

import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandRouter {

    private final UserStateService userStateService;
    private final Map<String, CommandHandler> handlers;
    private final Map<BotState, StatefulCommandHandler> statefulHandlers;
    private final CommandHandler defaultHandler;

    public CommandRouter(UserStateService userStateService, List<CommandHandler> allHandlers) {
        this.userStateService = userStateService;
        this.handlers = allHandlers.stream()
                .filter(h -> h.command() != null)
                .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
        this.statefulHandlers = allHandlers.stream()
                .filter(h -> h instanceof StatefulCommandHandler)
                .map(h -> (StatefulCommandHandler) h)
                .flatMap(h -> h.handledStates().stream().map(state -> Map.entry(state, h)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.defaultHandler = allHandlers.stream()
                .filter(h -> h.command() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default command handler registered"));
    }

    public SendMessage route(Update update) {
        if (!update.hasMessage()) return null;
        if (!update.getMessage().hasText() && !update.getMessage().hasDocument()) return null;
        if (update.getMessage().getFrom() == null) return null;

        long userId = update.getMessage().getFrom().getId();
        BotState state = userStateService.getState(userId);

        if (update.getMessage().hasDocument()) return routeDocument(update, userId, state);
        return routeText(update, userId, state);
    }

    private SendMessage routeDocument(Update update, long userId, BotState state) {
        StatefulCommandHandler h = statefulHandlers.get(state);
        if (h == null) {
            if (state != BotState.IDLE) {
                return MessageBuilder.html(update.getMessage().getChatId(),
                        Messages.get(userStateService.getLanguage(userId), Messages.USE_BUTTONS));
            }
            return null;
        }
        return h.handleState(update);
    }

    private SendMessage routeText(Update update, long userId, BotState state) {
        String text = update.getMessage().getText().trim();
        String command = text.split("[\\s@]")[0];

        if ("/cancel".equals(command) || handlers.containsKey(command)) {
            return handlers.getOrDefault(command, defaultHandler).handle(update);
        }

        String mappedCommand = MessageBuilder.LABEL_TO_COMMAND.get(text);
        if (mappedCommand != null) {
            userStateService.clearState(userId);
            return handlers.getOrDefault(mappedCommand, defaultHandler).handle(update);
        }

        StatefulCommandHandler h = statefulHandlers.get(state);
        if (h != null) {
            return h.handleState(update);
        }

        if (state != BotState.IDLE) {
            return MessageBuilder.html(update.getMessage().getChatId(),
                    Messages.get(userStateService.getLanguage(userId), Messages.USE_BUTTONS));
        }

        return handlers.getOrDefault(command, defaultHandler).handle(update);
    }
}

---

## SNIPPET: src/main/java/com/festiva/bot/CallbackQueryHandler.java (first 320 lines)

package com.festiva.bot;

import com.festiva.command.DatePickerKeyboard;
import com.festiva.command.MessageBuilder;
import com.festiva.command.handler.BulkAddCommandHandler;
import com.festiva.command.handler.DeleteAccountCommandHandler;
import com.festiva.command.handler.EditFriendCommandHandler;
import com.festiva.command.handler.ImportIcsCommandHandler;
import com.festiva.command.handler.ListCommandHandler;
import com.festiva.command.handler.RemoveCommandHandler;
import com.festiva.command.handler.SettingsCommandHandler;
import com.festiva.command.handler.UpcomingBirthdaysCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import com.festiva.util.UserDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    public static final String ACTION_ADD   = "ACTION_ADD";
    public static final String ACTION_ABOUT = "ACTION_ABOUT";

    private static final String MONTH_PREFIX   = "MONTH_";
    private static final String REMOVE_PREFIX  = "REMOVE_";
    private static final String CONFIRM_PREFIX = "CONFIRM_REMOVE_";
    private static final String CANCEL_REMOVE  = "CANCEL_REMOVE";
    private static final String LANG_PREFIX    = "LANG_";
    private static final String CURRENT_MONTH  = "CURRENT";
    private static final String LIST_SORT_DATE = "LIST_SORT_DATE";
    private static final String LIST_SORT_NAME = "LIST_SORT_NAME";

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final UpcomingBirthdaysCommandHandler upcomingHandler;
    private final ListCommandHandler listHandler;
    private final BulkAddCommandHandler bulkAddHandler;
    private final DatePickerCallbackHandler datePickerHandler;
    private final EditCallbackHandler editHandler;
    private final RemoveCommandHandler removeCommandHandler;
    private final EditFriendCommandHandler editFriendCommandHandler;
    private final DeleteAccountCommandHandler deleteAccountHandler;
    private final BotCommandsService commandsService;
    private final UserDateService userDateService;

    public EditMessageText handle(CallbackQuery callbackQuery) {
        if (callbackQuery == null) {
            return null;
        }

        String data = callbackQuery.getData();
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (data == null || message == null) {
            return null;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        long userId = callbackQuery.getFrom().getId();
        Lang lang = userStateService.getLanguage(userId);

        CallbackResult result = dispatch(data, chatId, userId, lang);
        if (result == null) {
            return null;
        }
        if (result.sendMessage != null) {
            return toEdit(result.sendMessage, messageId);
        }

        EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode("HTML")
                .text(result.text != null ? result.text : "");
        if (result.markup != null) {
            builder.replyMarkup(result.markup);
        }
        return builder.build();
    }

    private CallbackResult dispatch(String data, long chatId, long userId, Lang lang) {
        CallbackResult r;
        if ((r = dispatchDatePicker(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchEdit(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchRemove(data, userId, lang)) != null) {
            return r;
        }
        if ((r = dispatchMisc(data, chatId, userId, lang)) != null) {
            return r;
        }

        log.warn("callback.unknown: userId={}, data={}", userId, data);
        return null;
    }

    private CallbackResult dispatchDatePicker(String data, long userId, Lang lang) {
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PAGE_PREFIX)) {
            return datePickerHandler.handleYearPage(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_YEAR_PREFIX)) {
            return datePickerHandler.handleYearPick(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_SKIP_YEAR.equals(data)) {
            return datePickerHandler.handleSkipYear(userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_MONTH_PREFIX)) {
            return datePickerHandler.handleMonthPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_DAY_PREFIX)) {
            return datePickerHandler.handleDayPick(data, userId, lang);
        }
        if (data.startsWith(DatePickerKeyboard.DATE_BACK_TO_YEAR)) {
            return datePickerHandler.handleBackToYear(data, userId, lang);
        }
        if (DatePickerKeyboard.DATE_BACK_TO_MONTH.equals(data)) {
            return datePickerHandler.handleBackToMonth(userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.RELATIONSHIP_PREFIX)) {
            return datePickerHandler.handleRelationship(data, userId, lang);
        }
        if (data.startsWith(DatePickerCallbackHandler.EDIT_REL_PREFIX)) {
            return datePickerHandler.handleEditRelationship(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchEdit(String data, long userId, Lang lang) {
        if (data.startsWith(EditFriendCommandHandler.EDIT_PAGE_PREFIX)) {
            return handleEditPage(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NOTIFY)) {
            return editHandler.handleEditNotify(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_NAME)) {
            return editHandler.handleEditFieldName(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_DATE)) {
            return editHandler.handleEditFieldDate(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_FIELD_REL)) {
            return datePickerHandler.handleEditFieldRel(data, userId, lang);
        }
        if (data.startsWith(EditCallbackHandler.EDIT_PREFIX)) {
            return editHandler.handleEditSelect(data, userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchRemove(String data, long userId, Lang lang) {
        if (data.startsWith(RemoveCommandHandler.REMOVE_PAGE_PREFIX)) {
            return handleRemovePage(data, userId, lang);
        }
        if (data.startsWith(CONFIRM_PREFIX)) {
            return handleConfirmRemove(userId, data.substring(CONFIRM_PREFIX.length()), lang);
        }
        if (data.startsWith(REMOVE_PREFIX)) {
            return handleRemove(data, userId, lang);
        }
        if (CANCEL_REMOVE.equals(data)) {
            return handleCancelRemove(userId, lang);
        }
        return null;
    }

    private CallbackResult dispatchMisc(String data, long chatId, long userId, Lang lang) {
        if (data.startsWith(SettingsCommandHandler.SETTINGS_HOUR_PREFIX)) {
            return handleSettingsHour(data, userId, lang);
        }
        if (data.startsWith(SettingsCommandHandler.SETTINGS_TZ_PREFIX)) {
            return handleSettingsTz(data, userId, lang);
        }
        if (data.startsWith(UpcomingBirthdaysCommandHandler.UPCOMING_DAYS_PREFIX)) {
            return handleUpcoming(data, userId, lang);
        }
        if (data.startsWith(ListCommandHandler.LIST_PAGE_PREFIX)) {
            return handleListPage(data, userId, lang);
        }
        if (data.startsWith(LIST_SORT_DATE) || data.startsWith(LIST_SORT_NAME)) {
            return handleListSort(data, userId, lang);
        }
        if (data.startsWith(LANG_PREFIX)) {
            return handleLanguage(userId, data.substring(LANG_PREFIX.length()));
        }
        if (data.startsWith(MONTH_PREFIX)) {
            return handleMonth(userId, data, lang);
        }

        switch (data) {
            case ACTION_ADD -> {
                return handleActionAdd(userId, lang);
            }
            case ACTION_ABOUT -> {
                return new CallbackResult(Messages.get(lang, Messages.ABOUT), null);
            }
            case BulkAddCommandHandler.CALLBACK_PASTE -> {
                return new CallbackResult(bulkAddHandler.promptPaste(chatId, userId, lang));
            }
            case BulkAddCommandHandler.CALLBACK_CSV -> {
                bulkAddHandler.sendCsvTemplate(chatId, lang);
                return null;
            }
            case BulkAddCommandHandler.CALLBACK_ICS -> {
                userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
                return new CallbackResult(Messages.get(lang, Messages.ICS_PROMPT), null);
            }
            case DeleteAccountCommandHandler.CONFIRM_DELETE -> {
                return handleConfirmDeleteAccount(userId, lang);
            }
            case DeleteAccountCommandHandler.CANCEL_DELETE -> {
                return new CallbackResult(Messages.get(lang, Messages.DELETE_ACCOUNT_CANCEL), null);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CONFIRM -> {
                return handleIcsConfirm(userId, lang);
            }
            case ImportIcsCommandHandler.CALLBACK_ICS_CANCEL -> {
                userStateService.clearState(userId);
                return new CallbackResult(Messages.get(lang, Messages.ICS_CANCELLED), null);
            }
            default -> {
                return null;
            }
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    private CallbackResult handleSettingsHour(String data, long userId, Lang lang) {
        try {
            int hour = Integer.parseInt(data.substring(SettingsCommandHandler.SETTINGS_HOUR_PREFIX.length()));
            if (hour < 0 || hour > 23) {
                log.warn("callback.settings.hour.invalid: userId={}, hour={}", userId, hour);
                return sessionExpired(lang);
            }
            userStateService.setNotifyHour(userId, hour);
            return new CallbackResult(Messages.get(lang, Messages.SETTINGS_HOUR_SET, hour),
                    SettingsCommandHandler.combined(hour, userStateService.getTimezone(userId)));
        } catch (NumberFormatException e) {
            log.warn("callback.settings.hour.parse.failed: data={}", data, e);
            return sessionExpired(lang);
        }
    }

    private CallbackResult handleSettingsTz(String data, long userId, Lang lang) {
        String tz = data.substring(SettingsCommandHandler.SETTINGS_TZ_PREFIX.length());
        try {
            @SuppressWarnings("unused")
            java.time.ZoneId validatedZone = java.time.ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.warn("callback.settings.tz.invalid: userId={}, tz={}", userId, tz, e);
            return sessionExpired(lang);
        }
        userStateService.setTimezone(userId, tz);
        return new CallbackResult(Messages.get(lang, Messages.SETTINGS_TZ_SET, tz),
                SettingsCommandHandler.combined(userStateService.getNotifyHour(userId), tz));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    private CallbackResult handleListSort(String data, long userId, Lang lang) {
        boolean byDate = data.startsWith(LIST_SORT_DATE);
        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private CallbackResult handleListPage(String data, long userId, Lang lang) {
        String suffix = data.substring(ListCommandHandler.LIST_PAGE_PREFIX.length());
        boolean byDate;
        if (suffix.startsWith("DATE_")) {
            byDate = true;
        } else if (suffix.startsWith("NAME_")) {
            byDate = false;
        } else {
            log.warn("callback.list.page.invalid.mode: data={}", data);
            return sessionExpired(lang);
        }

        Integer page = parsePageSuffix(data);
        if (page == null) {
            return sessionExpired(lang);
        }

        var friends = friendService.getFriendsSortedByDayMonth(userId);
        return new CallbackResult(listHandler.buildText(friends, lang, byDate, page, userId),
                listHandler.keyboard(lang, byDate, page, friends.size()));
    }

    private Integer parsePageSuffix(String data) {
        int idx = data.lastIndexOf('_');
        if (idx < 0) {


---

## SNIPPET: src/main/java/com/festiva/friend/entity/Friend.java (first 240 lines)

package com.festiva.friend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.util.Locale;

@Data
@Document(collection = "friends")
@NoArgsConstructor
@CompoundIndex(name = "user_normalized_name", def = "{'telegramUserId': 1, 'normalizedName': 1}", unique = true)
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    private String normalizedName;

    private Integer birthYear;
    private int birthMonth;
    private int birthDay;

    private boolean notifyEnabled = true;
    private Relationship relationship;

    public Friend(String name, LocalDate birthDate) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
    }

    public Friend(String name, LocalDate birthDate, Relationship relationship) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
        this.relationship = relationship;
    }

    public Friend(String name, Integer year, int month, int day) {
        String sanitizedName = name == null ? null : name.trim();
        if (sanitizedName == null || sanitizedName.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitizedName.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }

        this.name = sanitizedName;
        this.normalizedName = normalizeName(sanitizedName);

        try {
            if (year != null) {
                @SuppressWarnings("unused")
                LocalDate validDate = LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                MonthDay validMonthDay = MonthDay.of(month, day);
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date: " +
                    (year != null ? year + "-" : "") + month + "-" + day, e);
        }

        this.birthYear = year;
        this.birthMonth = month;
        this.birthDay = day;
    }

    public Friend(String name, Integer year, int month, int day, Relationship relationship) {
        this(name, year, month, day);
        this.relationship = relationship;
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
        this.normalizedName = normalizeName(this.name);
    }

    public boolean hasYear() {
        return birthYear != null;
    }

    public MonthDay getBirthMonthDay() {
        return MonthDay.of(birthMonth, birthDay);
    }

    public LocalDate getBirthDate() {
        if (!hasYear()) {
            throw new IllegalStateException("Birth year is unknown for " + name);
        }
        return LocalDate.of(birthYear, birthMonth, birthDay);
    }

    public int getAge(LocalDate on) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return Period.between(getBirthDate(), on).getYears();
    }

    public LocalDate nextBirthday(LocalDate from) {
        boolean isLeapDayBirthday = (birthMonth == 2 && birthDay == 29);

        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            if (hasYear()) {
                int year = from.getYear();
                while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                    year++;
                }
                next = LocalDate.of(year, 2, 29);
            } else {
                next = LocalDate.of(from.getYear(), 2, 28);
            }
        }

        if (next.isBefore(from)) {
            if (isLeapDayBirthday) {
                if (hasYear()) {
                    int year = from.getYear() + 1;
                    while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                        year++;
                    }
                    next = LocalDate.of(year, 2, 29);
                } else {
                    int year = from.getYear() + 1;
                    next = LocalDate.of(year, 1, 1).isLeapYear()
                            ? LocalDate.of(year, 2, 29)
                            : LocalDate.of(year, 2, 28);
                }
            } else {
                next = LocalDate.of(from.getYear() + 1, birthMonth, birthDay);
            }
        }

        return next;
    }

    public int getNextAge(LocalDate from) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return nextBirthday(from).getYear() - birthYear;
    }

    private static final int[][] ZODIAC_ENDS = {{1,19},{2,18},{3,20},{4,19},{5,20},{6,20},{7,22},{8,22},{9,22},{10,22},{11,21},{12,21}};
    private static final String[] ZODIAC_SIGNS = {"♑","♒","♓","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑"};

    public String getZodiac() {
        int m = birthMonth;
        int d = birthDay;
        int idx = d <= ZODIAC_ENDS[m - 1][1] ? m - 1 : m;
        return ZODIAC_SIGNS[idx];
    }
}


---

## SNIPPET: src/main/java/com/festiva/friend/api/FriendService.java (first 260 lines)

package com.festiva.friend.api;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.repository.FriendMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    public static final int FRIEND_CAP = 100;
    public static final int JUBILEE_INTERVAL = 5;
    private static final int LEAP_YEAR = 2000;

    private final FriendMongoRepository friendRepository;

    public void addFriend(long telegramUserId, Friend friend) {
        if (friend == null) {
            throw new IllegalArgumentException("Friend cannot be null");
        }

        String sanitizedName = sanitizeName(friend.getName());
        friend.setTelegramUserId(telegramUserId);
        friend.setName(sanitizedName);

        try {
            friendRepository.save(friend);
        } catch (DuplicateKeyException e) {
            log.warn("friend.create.rejected.duplicate: userId={}", telegramUserId);
            throw new IllegalArgumentException("Friend with this name already exists", e);
        }
    }

    public boolean friendExists(long telegramUserId, String name) {
        return friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, Friend.normalizeName(name));
    }

    public void deleteFriend(long telegramUserId, String name) {
        friendRepository.deleteByTelegramUserIdAndNameIgnoreCase(telegramUserId, name);
    }

    public void deleteFriendById(String id, long telegramUserId) {
        friendRepository.deleteByIdAndTelegramUserId(id, telegramUserId);
    }

    public java.util.Optional<Friend> findOwnedFriend(String id, long telegramUserId) {
        return friendRepository.findByIdAndTelegramUserId(id, telegramUserId);
    }

    public void deleteAllFriends(long telegramUserId) {
        friendRepository.deleteByTelegramUserId(telegramUserId);
    }

    public void updateFriendNameById(String id, long telegramUserId, String newName) {
        String sanitizedName = sanitizeName(newName);
        findOwnedFriend(id, telegramUserId).ifPresent(friend -> {
            String newNormalized = Friend.normalizeName(sanitizedName);
            String currentNormalized = Friend.normalizeName(friend.getName());

            if (!newNormalized.equals(currentNormalized)
                    && friendRepository.existsByTelegramUserIdAndNormalizedName(telegramUserId, newNormalized)) {
                throw new IllegalArgumentException("Friend with this name already exists");
            }

            friend.setName(sanitizedName);
            try {
                friendRepository.save(friend);
            } catch (DuplicateKeyException e) {
                log.warn("friend.update.rejected.duplicate: userId={}, friendId={}", telegramUserId, id);
                throw new IllegalArgumentException("Friend with this name already exists", e);
            }
        });
    }

    public void updateFriendDateById(String id, long telegramUserId, Integer year, int month, int day) {
        var existing = findOwnedFriend(id, telegramUserId);
        if (existing.isEmpty()) {
            return;
        }

        try {
            if (year != null) {
                @SuppressWarnings("unused")
                java.time.LocalDate validDate = java.time.LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                java.time.MonthDay validMonthDay = java.time.MonthDay.of(month, day);
            }
        } catch (java.time.DateTimeException e) {
            log.warn("friend.date.update.rejected.invalid: userId={}, friendId={}, hasYear={}",
                    telegramUserId, id, year != null, e);
            throw new IllegalArgumentException("Invalid date: " +
                    (year != null ? year + "-" : "") + month + "-" + day, e);
        }

        Friend friend = existing.get();
        friend.setBirthYear(year);
        friend.setBirthMonth(month);
        friend.setBirthDay(day);
        friendRepository.save(friend);
    }

    public void updateFriendRelationshipById(String id, long telegramUserId, com.festiva.friend.entity.Relationship relationship) {
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
            f.setRelationship(relationship);
            friendRepository.save(f);
        });
    }

    public boolean toggleFriendNotifyById(String id, long telegramUserId) {
        var ref = new Object() { boolean newValue = true; };
        findOwnedFriend(id, telegramUserId).ifPresent(f -> {
            f.setNotifyEnabled(!f.isNotifyEnabled());
            friendRepository.save(f);
            ref.newValue = f.isNotifyEnabled();
        });
        return ref.newValue;
    }

    public List<Friend> getFriends(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId);
    }

    public List<Friend> getFriendsSortedByDayMonth(long telegramUserId) {
        return friendRepository.findByTelegramUserId(telegramUserId).stream()
                .sorted(Comparator.comparing(f -> java.time.LocalDate.of(LEAP_YEAR, f.getBirthMonth(), f.getBirthDay())))
                .toList();
    }

    public List<Long> getAllUserIds() {
        return friendRepository.findDistinctTelegramUserIds();
    }

    public Map<Long, List<Friend>> getFriendsByUserIds(List<Long> userIds) {
        return friendRepository.findByTelegramUserIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Friend::getTelegramUserId));
    }

    private String sanitizeName(String name) {
        String sanitized = name == null ? null : name.trim();
        if (sanitized == null || sanitized.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }
        return sanitized;
    }
}


---

## SNIPPET: src/main/java/com/festiva/state/BotState.java (first 120 lines)

package com.festiva.state;

public enum BotState {

    IDLE,
    WAITING_FOR_ADD_FRIEND_NAME,
    WAITING_FOR_ADD_FRIEND_DATE,
    WAITING_FOR_ADD_FRIEND_RELATIONSHIP,
    WAITING_FOR_REMOVE_CONFIRM,
    WAITING_FOR_EDIT_NAME,
    WAITING_FOR_EDIT_DATE,
    WAITING_FOR_SEARCH,
    WAITING_FOR_BULK_ADD,
    WAITING_FOR_EDIT_RELATIONSHIP,
    WAITING_FOR_ICS_FILE,
    WAITING_FOR_ICS_CONFIRM
}


---

## SNIPPET: src/main/java/com/festiva/state/UserStateService.java (first 260 lines)

package com.festiva.state;

import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserStateService {

    private final Cache<Long, UserSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();
    private final UserSessionRepository sessionRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PendingImportRepository pendingImportRepository;

    private UserSession session(long userId) {
        return cache.get(userId, id -> {
            UserSession session = sessionRepository.findById(id).orElse(new UserSession());
            session.setUserId(id);
            session.touch();
            
            // Reject stale sessions from DB
            if (session.getLastActivity() != null && 
                session.getLastActivity().isBefore(LocalDateTime.now().minusHours(1))) {
                session = new UserSession();
                session.setUserId(id);
                session.touch();
            }
            
            return session;
        });
    }
    
    private void saveSession(long userId) {
        UserSession session = cache.getIfPresent(userId);
        if (session != null) {
            session.touch();
            sessionRepository.save(session);
        }
    }

    public BotState getState(long userId) { return session(userId).getState(); }
    public void setState(long userId, BotState state) { 
        session(userId).setState(state);
        saveSession(userId);
    }

    public void clearState(long userId) {
        UserSession s = session(userId);
        s.setState(BotState.IDLE);
        s.setPendingName(null);
        s.setPendingId(null);
        s.setPendingYear(null);
        s.setPendingMonth(null);
        s.setPendingDay(null);
        s.setYearPageOffset(0);
        pendingImportRepository.deleteByUserId(userId);
        saveSession(userId);
    }

    public void removeSession(long userId) {
        cache.invalidate(userId);
        sessionRepository.deleteById(userId);
    }

    public void setPendingName(long userId, String name) { 
        session(userId).setPendingName(name);
        saveSession(userId);
    }
    public String getPendingName(long userId) { return session(userId).getPendingName(); }

    public void setPendingId(long userId, String id) { 
        session(userId).setPendingId(id);
        saveSession(userId);
    }
    public String getPendingId(long userId) { return session(userId).getPendingId(); }

    public void setPendingYear(long userId, Integer year) { 
        session(userId).setPendingYear(year);
        saveSession(userId);
    }
    public Integer getPendingYear(long userId) { return session(userId).getPendingYear(); }

    public void setPendingMonth(long userId, Integer month) { 
        session(userId).setPendingMonth(month);
        saveSession(userId);
    }
    public Integer getPendingMonth(long userId) { return session(userId).getPendingMonth(); }

    public void setYearPageOffset(long userId, int offset) { 
        session(userId).setYearPageOffset(offset);
        saveSession(userId);
    }
    public int getYearPageOffset(long userId) { return session(userId).getYearPageOffset(); }

    public void setPendingDay(long userId, Integer day) { 
        session(userId).setPendingDay(day);
        saveSession(userId);
    }
    public Integer getPendingDay(long userId) { return session(userId).getPendingDay(); }

    public void setPendingIcsImport(long userId, java.util.List<Friend> friends) {
        pendingImportRepository.deleteByUserId(userId);
        if (friends != null && !friends.isEmpty()) {
            pendingImportRepository.save(new PendingImport(userId, friends));
        }
    }
    
    public java.util.List<Friend> getPendingIcsImport(long userId) {
        return pendingImportRepository.findByUserId(userId)
                .map(PendingImport::getFriends)
                .orElse(null);
    }

    public Lang getLanguage(long userId) {
        UserSession s = session(userId);
        if (s.getLang() == null) {
            Lang lang = userPreferenceRepository.findById(userId)
                    .map(UserPreference::getLang)
                    .orElse(UserPreference.DEFAULT_LANG);
            s.setLang(lang);
        }
        return s.getLang();
    }

    public void setLanguage(long userId, Lang lang) {
        session(userId).setLang(lang);
        saveSession(userId);
        UserPreference pref = getOrCreatePref(userId);
        pref.setLang(lang);
        userPreferenceRepository.save(pref);
    }

    public int getNotifyHour(long userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::getNotifyHour)
                .orElse(9);
    }

    public void setNotifyHour(long userId, int hour) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setNotifyHour(hour);
        userPreferenceRepository.save(pref);
    }

    public String getTimezone(long userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::getTimezone)
                .orElse(UserPreference.DEFAULT_TIMEZONE);
    }

    public void setTimezone(long userId, String timezone) {
        UserPreference pref = getOrCreatePref(userId);
        pref.setTimezone(timezone);
        userPreferenceRepository.save(pref);
    }

    private UserPreference getOrCreatePref(long userId) {
        return userPreferenceRepository.findById(userId)
                .orElse(new UserPreference(userId, UserPreference.DEFAULT_LANG, 9, UserPreference.DEFAULT_TIMEZONE, null));
    }
}


---

## SNIPPET: src/main/java/com/festiva/user/UserPreference.java (first 120 lines)

package com.festiva.user;

import com.festiva.i18n.Lang;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user_preferences")
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final Lang DEFAULT_LANG = Lang.EN;

    @Id
    private long telegramUserId;
    private Lang lang = DEFAULT_LANG;
    private int notifyHour = 9;
    private String timezone = DEFAULT_TIMEZONE;
    private java.time.LocalDate lastNotifiedDate;
}


---

## SNIPPET: src/main/java/com/festiva/notification/BirthdayReminder.java (first 260 lines)

package com.festiva.notification;

import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.user.UserPreference;
import com.festiva.user.UserPreferenceRepository;
import com.festiva.util.HtmlEscaper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayReminder {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${festiva.reminder.startup-check.enabled:true}")
    private boolean startupCheckEnabled;

    @Value("${festiva.reminder.schedule.enabled:true}")
    private boolean scheduleEnabled;

    private static final Map<Long, String> TEMPLATE_KEYS = Map.of(
            0L, Messages.NOTIFY_TODAY,
            1L, Messages.NOTIFY_TOMORROW,
            7L, Messages.NOTIFY_WEEK
    );

    private static final Map<Long, String> TEMPLATE_KEYS_NO_YEAR = Map.of(
            0L, Messages.NOTIFY_TODAY_NO_YEAR,
            1L, Messages.NOTIFY_TOMORROW_NO_YEAR,
            7L, Messages.NOTIFY_WEEK_NO_YEAR
    );

    private final FriendService friendService;
    private final NotificationSender notificationSender;
    private final UserPreferenceRepository userPreferenceRepository;

    @PostConstruct
    public void checkBirthdaysOnStartup() {
        if (!startupCheckEnabled) {
            return;
        }

        try {
            checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
        } catch (Exception e) {
            log.warn("reminder.startup.check.failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void checkBirthdays() {
        if (!scheduleEnabled) {
            return;
        }

        checkBirthdaysForHour(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    void checkBirthdaysForHour(ZonedDateTime utcNow) {
        List<Long> userIds = friendService.getAllUserIds();

        Map<Long, UserPreference> prefByUser = userPreferenceRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserPreference::getTelegramUserId, p -> p));
        Map<Long, List<Friend>> friendsByUser = friendService.getFriendsByUserIds(userIds);

        userIds.forEach(userId -> {
            MDC.put("userId", String.valueOf(userId));
            try {
                processUser(userId, prefByUser.get(userId), friendsByUser.getOrDefault(userId, List.of()), utcNow);
            } finally {
                MDC.remove("userId");
            }
        });
    }

    private void processUser(long userId, UserPreference pref, List<Friend> friends, ZonedDateTime utcNow) {
        ZoneId zone = resolveZone(pref);
        if (zone == null) {
            return;
        }

        ZonedDateTime userNow = utcNow.withZoneSameInstant(zone);
        if (!shouldNotify(pref, userNow)) {
            return;
        }

        LocalDate today = userNow.toLocalDate();
        Lang lang = pref != null && pref.getLang() != null ? pref.getLang() : UserPreference.DEFAULT_LANG;

        int count = (int) friends.stream().filter(f -> checkAndNotify(userId, f, today, lang)).count();
        if (count > 0) {
            UserPreference p = pref != null ? pref : new UserPreference();
            p.setTelegramUserId(userId);
            p.setLastNotifiedDate(today);
            userPreferenceRepository.save(p);
        }
    }

    private ZoneId resolveZone(UserPreference pref) {
        String tz = pref != null && pref.getTimezone() != null ? pref.getTimezone() : UserPreference.DEFAULT_TIMEZONE;
        try {
            return ZoneId.of(tz);
        } catch (java.time.zone.ZoneRulesException e) {
            log.warn("reminder.timezone.invalid: tz={}", tz, e);
            return null;
        }
    }

    private boolean shouldNotify(UserPreference pref, ZonedDateTime userNow) {
        int notifyHour = pref != null && pref.getNotifyHour() >= 0 && pref.getNotifyHour() <= 23 ? pref.getNotifyHour() : 9;
        if (notifyHour != userNow.getHour()) {
            return false;
        }
        LocalDate today = userNow.toLocalDate();
        return !today.equals(pref != null ? pref.getLastNotifiedDate() : null);
    }

    private boolean checkAndNotify(long userId, Friend friend, LocalDate today, Lang lang) {
        if (!friend.isNotifyEnabled()) {
            return false;
        }

        long daysUntil = ChronoUnit.DAYS.between(today, friend.nextBirthday(today));

        Map<Long, String> templates = friend.hasYear() ? TEMPLATE_KEYS : TEMPLATE_KEYS_NO_YEAR;
        String key = templates.get(daysUntil);
        if (key == null) {
            return false;
        }

        try {
            String message;
            if (friend.hasYear()) {
                message = Messages.get(lang, key,
                        HtmlEscaper.escape(friend.getName()),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        Messages.yearsRu(lang, friend.getNextAge(today)),
                        botUsername);
            } else {
                message = Messages.get(lang, key,
                        HtmlEscaper.escape(friend.getName()),
                        friend.getRelationship() != null ? " " + friend.getRelationship().label(lang) : "",
                        friend.getZodiac(),
                        botUsername);
            }

            notificationSender.send(userId, message);
            return true;
        } catch (RuntimeException e) {
            log.error("reminder.notify.failed: userId={}, friendId={}, daysUntil={}",
                    userId, friend.getId(), daysUntil, e);
            return false;
        }
    }
}

---

## SNIPPET: src/main/java/com/festiva/command/handler/ImportIcsCommandHandler.java (first 320 lines)

package com.festiva.command.handler;

import com.festiva.ai.IcsNameExtractorService;
import com.festiva.command.MessageBuilder;
import com.festiva.command.StatefulCommandHandler;
import com.festiva.friend.api.FriendService;
import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;
import com.festiva.state.BotState;
import com.festiva.state.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportIcsCommandHandler implements StatefulCommandHandler {

    public static final String CALLBACK_ICS_CONFIRM = "ICS_CONFIRM";
    public static final String CALLBACK_ICS_CANCEL  = "ICS_CANCEL";

    private static final DateTimeFormatter ICS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private static final int FILE_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int FILE_READ_TIMEOUT_MILLIS = 10_000;

    private final FriendService friendService;
    private final UserStateService userStateService;
    private final TelegramClient telegramClient;

    @Autowired(required = false)
    private IcsNameExtractorService icsNameExtractorService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String command() {
        return "/importics";
    }

    @Override
    public Set<BotState> handledStates() {
        return Set.of(BotState.WAITING_FOR_ICS_FILE);
    }

    @Override
    public SendMessage handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_FILE);
        return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PROMPT));
    }

    @Override
    public SendMessage handleState(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        Lang lang = userStateService.getLanguage(userId);

        if (!update.getMessage().hasDocument()) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NOT_A_FILE));
        }

        var doc = update.getMessage().getDocument();
        String mime = doc.getMimeType();
        if (mime != null && !mime.startsWith("text/") && !mime.equals("application/octet-stream")) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_WRONG_TYPE));
        }
        if (doc.getFileSize() != null && doc.getFileSize() > 15_000_000) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_TOO_LARGE));
        }

        List<String> lines = downloadLines(doc.getFileId());
        if (lines == null) {
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_PARSE_ERROR));
        }

        List<IcsEntry> entries = extractYearlyEntries(lines);
        if (entries.isEmpty()) {
            userStateService.clearState(userId);
            return MessageBuilder.html(chatId, Messages.get(lang, Messages.ICS_NO_EVENTS));
        }

        List<Friend> candidates = new ArrayList<>();
        for (IcsEntry entry : entries) {
            try {
                candidates.add(convertToFriend(entry));
            } catch (IllegalArgumentException e) {
                log.warn("ics.import.entry.skipped.invalid: summary={}", entry.summary(), e);
            }
        }

        Set<String> existing = friendService.getFriends(userId).stream()
                .map(friend -> Friend.normalizeName(friend.getName()))
                .collect(Collectors.toSet());

        List<Friend> toSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new java.util.HashSet<>();

        for (Friend friend : candidates) {
            String normalized = Friend.normalizeName(friend.getName());
            if (existing.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_EXISTS, 0, friend.getName()));
            } else if (seenInBatch.contains(normalized)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DUPLICATE, 0, friend.getName()));
            } else if (friend.getName().length() > 100) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_NAME_LONG, 0));
            } else {
                seenInBatch.add(normalized);
                toSave.add(friend);
            }
        }

        int currentCount = existing.size();
        if (currentCount + toSave.size() > FriendService.FRIEND_CAP) {
            int allowed = Math.max(0, FriendService.FRIEND_CAP - currentCount);
            if (allowed < toSave.size()) {
                errors.add(Messages.get(lang, Messages.BULK_CAP_EXCEEDED, allowed, FriendService.FRIEND_CAP));
                toSave = toSave.subList(0, allowed);
            }
        }

        if (toSave.isEmpty()) {
            userStateService.clearState(userId);
            String preview = buildPreviewLines(List.of(), errors);
            return MessageBuilder.html(chatId,
                    Messages.get(lang, Messages.ICS_PREVIEW_NO_VALID, entries.size(), preview));
        }

        userStateService.setPendingIcsImport(userId, toSave);
        userStateService.setState(userId, BotState.WAITING_FOR_ICS_CONFIRM);

        String preview = buildPreviewLines(toSave, errors);
        String text = Messages.get(lang, Messages.ICS_PREVIEW, entries.size(), preview, toSave.size());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CONFIRM_BTN, toSave.size()))
                                .callbackData(CALLBACK_ICS_CONFIRM).build(),
                        InlineKeyboardButton.builder()
                                .text(Messages.get(lang, Messages.ICS_CANCEL_BTN))
                                .callbackData(CALLBACK_ICS_CANCEL).build()
                ))).build();
        return MessageBuilder.html(chatId, text, kb);
    }

    public record IcsEntry(String summary, LocalDate date, boolean yearTrusted) {}

    public static List<IcsEntry> extractYearlyEntries(List<String> raw) {
        List<String> unfolded = unfold(raw);
        List<IcsEntry> result = new ArrayList<>();
        String summary = null;
        String dtstart = null;
        boolean inEvent = false;
        boolean yearly = false;
        boolean isBirthday = false;

        for (String line : unfolded) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true;
                summary = null;
                dtstart = null;
                yearly = false;
                isBirthday = false;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && dtstart != null) {
                    boolean accept = yearly || isBirthday ||
                            (summary != null && summary.toLowerCase(Locale.ROOT).matches(".*(birthday|bday|born).*"));

                    if (accept && summary != null) {
                        LocalDate date = parseIcsDate(dtstart);
                        if (date != null) {
                            boolean yearTrusted = date.isBefore(LocalDate.now()) &&
                                    date.getYear() > 1900 &&
                                    date.getYear() < LocalDate.now().getYear();
                            result.add(new IcsEntry(summary.trim(), date, yearTrusted));
                        }
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                if (line.startsWith("SUMMARY:")) {
                    summary = line.substring(8);
                } else if (line.startsWith("DTSTART")) {
                    int colon = line.indexOf(':');
                    if (colon >= 0) {
                        dtstart = line.substring(colon + 1).trim();
                    }
                } else if (line.startsWith("RRULE:") && line.contains("FREQ=YEARLY")) {
                    yearly = true;
                } else if (line.startsWith("X-GOOGLE-CALENDAR-CONTENT-TYPE:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("CATEGORIES:")
                        && line.toLowerCase(Locale.ROOT).contains("birthday")) {
                    isBirthday = true;
                } else if (line.startsWith("BDAY:")) {
                    dtstart = line.substring(5).trim();
                    isBirthday = true;
                }
            }
        }
        return result;
    }

    public static List<String> extractYearlyCsvLines(List<String> raw) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
        return extractYearlyEntries(raw).stream()
                .map(entry -> entry.summary() + "," + entry.date().format(fmt))
                .toList();
    }

    private String resolveName(String summary) {
        if (icsNameExtractorService == null) {
            return summary == null ? null : summary.trim();
        }

        try {
            String extracted = icsNameExtractorService.extractName(summary);
            if (extracted == null || extracted.isBlank()) {
                return summary == null ? null : summary.trim();
            }
            return extracted.trim();
        } catch (Exception e) {
            log.warn("ics.ai.name.extraction.failed", e);
            return summary == null ? null : summary.trim();
        }
    }

    private Friend convertToFriend(IcsEntry entry) {
        String name = resolveName(entry.summary());
        LocalDate date = entry.date();

        if (!entry.yearTrusted()) {
            return new Friend(name, null, date.getMonthValue(), date.getDayOfMonth());
        }

        return new Friend(name, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static List<String> unfold(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                if (!out.isEmpty()) {
                    out.set(out.size() - 1, out.getLast() + line.substring(1));
                }
            } else {
                out.add(line.replace("\r", ""));
            }
        }
        return out;
    }

    private static LocalDate parseIcsDate(String value) {
        String datePart = value.length() >= 8 ? value.substring(0, 8) : value;
        try {
            return LocalDate.parse(datePart, ICS_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPreviewLines(List<Friend> valid, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (Friend friend : valid) {
            String dateStr = friend.hasYear()
                    ? friend.getBirthDate().format(CSV_DATE_FMT)
                    : String.format("%02d.%02d.", friend.getBirthMonthDay().getDayOfMonth(), friend.getBirthMonthDay().getMonthValue());
            sb.append("✅ ").append(friend.getName())
                    .append(" — ").append(dateStr).append("\n");
        }
        for (String error : errors) {
            sb.append("❌ ").append(error).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private List<String> downloadLines(String fileId) {
        try {
            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramClient.execute(GetFile.builder().fileId(fileId).build());

            String url = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(FILE_CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(FILE_READ_TIMEOUT_MILLIS);
            connection.setUseCaches(false);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {


---

## SNIPPET: src/main/java/com/festiva/ai/IcsNameExtractorService.java (first 120 lines)

package com.festiva.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface IcsNameExtractorService {

    @SystemMessage("""
            Extract only the person's name from a birthday event title.
            The title may be in any language (e.g. "День рождения Юли", "Birthday of John", "Happy birthday!").
            Return just the name, nothing else.
            If no name can be identified, return the original text unchanged.
            """)
    String extractName(@UserMessage String summary);
}

