# Telegram Bot Architecture — Best Practices

Distilled from the Festiva project. Language-agnostic principles with Java/Spring examples.
Apply these patterns in any language (Python, Go, TypeScript, etc.) and any domain.

---

## 1. Package by Feature, Not by Layer

Organise code around domain concepts, not technical roles.

```
src/
├── bot/          # entry point, update dispatch, callback handling
├── command/      # routing + all command handlers
├── friend/
│   ├── api/          # FriendService
│   ├── entity/       # Friend, Relationship
│   └── repository/   # FriendMongoRepository
├── i18n/         # Lang enum, Messages, MessageSourceConfig
├── metrics/      # MetricsSender interface + implementations
├── notification/ # BirthdayReminder scheduler + NotificationSender interface
├── state/        # BotState enum, UserStateService
└── user/         # UserPreference entity + repository
```

Each feature is self-contained. You can delete or replace a feature without touching others.

---

## 2. Command Handler — Strategy + Auto-Registry

Define a `CommandHandler` interface with two methods: the command it owns, and the handler.

```java
interface CommandHandler {
    String command();           // e.g. "/add", null = default fallback
    SendMessage handle(Update update);
}
```

`CommandRouter` collects all handlers at startup via DI and builds a `Map<String, CommandHandler>`.
No `if/else` chains. Adding a new command = adding a new class, nothing else changes.

```java
this.handlers = allHandlers.stream()
    .filter(h -> h.command() != null)
    .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
```

The default fallback handler has `command() = null` and is found with `findFirst()`.
It returns "unknown command" for anything not matched.

---

## 3. Stateful Command Handler — Extend the Strategy

Some commands span multiple messages (multi-step flows: add friend → enter name → pick date → pick relationship).
Extend the base interface with `StatefulCommandHandler` that declares which states it handles.

```java
interface StatefulCommandHandler extends CommandHandler {
    Set<BotState> handledStates();
    SendMessage handleState(Update update);
}
```

The router builds a second map `Map<BotState, StatefulCommandHandler>` from the same handler list.
Multi-step logic stays inside the handler that owns it, not scattered in the router.

---

## 4. Routing Priority Order

The router applies rules in a fixed, explicit priority order:

1. `/cancel` — always wins, clears state, regardless of current state
2. Exact command match (`/start`, `/add`, …)
3. Reply-keyboard label → mapped to a command (`"➕ Add"` → `/add`)
4. Active stateful state → delegate to the matching stateful handler
5. Default fallback handler (returns "unknown command")

Document this order. Test every case explicitly.

---

## 5. Finite State Machine for Conversations

Use an enum for all possible user states.

```java
enum BotState {
    IDLE,
    WAITING_FOR_ADD_FRIEND_NAME,
    WAITING_FOR_ADD_FRIEND_DATE,
    WAITING_FOR_ADD_FRIEND_RELATIONSHIP,
    WAITING_FOR_REMOVE_CONFIRM,
    WAITING_FOR_EDIT_NAME,
    WAITING_FOR_EDIT_DATE,
    WAITING_FOR_EDIT_RELATIONSHIP,
    WAITING_FOR_SEARCH,
    WAITING_FOR_BULK_ADD
}
```

Rules:
- Default state is always `IDLE`.
- Every handler that starts a flow sets the state. Every handler that ends a flow calls `clearState()`.
- `/cancel` always resets to `IDLE` and clears all pending data.
- State is stored in memory (`ConcurrentHashMap`) — fast, no DB round-trip per message.

---

## 6. Per-User Session Object

Store all transient, in-flight data in a single inner session object per user, not as separate maps.

```java
private static class UserSession {
    BotState state = BotState.IDLE;
    String pendingName = null;
    String pendingId   = null;
    Integer pendingYear, pendingMonth, pendingDay;
    int yearPageOffset = 0;
    Lang lang = null;
}

private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();
```

`UserStateService` is the single source of truth for in-memory session data.
Persistent preferences (language, timezone, notify hour) are written through to the DB on change and cached in the session on first read.

---

## 7. Callback Query Dispatch — Prefix-Based Routing

Inline keyboard buttons carry a `callbackData` string. Route them by prefix, not by equality.

```java
if (data.startsWith("DATE_YEAR_"))      return handleYearPick(data, ...);
if (data.startsWith("REMOVE_"))         return handleRemove(data, ...);
if (data.startsWith("CONFIRM_REMOVE_")) return handleConfirmRemove(data, ...);
```

Group related prefixes into private dispatch methods (`dispatchDatePicker`, `dispatchEdit`, `dispatchRemove`, `dispatchMisc`).
This keeps the top-level `handle()` method short and readable.

Naming convention for callback data: `DOMAIN_ACTION_PAYLOAD` — e.g. `REMOVE_abc123`, `LANG_EN`, `MONTH_3`, `SETTINGS_HOUR_9`.

Prefix constants live on the class that owns the handler, not on the dispatcher:
```java
// RemoveCommandHandler.java
public static final String REMOVE_PAGE_PREFIX = "REMOVE_PAGE_";

// SettingsCommandHandler.java
public static final String SETTINGS_HOUR_PREFIX = "SETTINGS_HOUR_";
public static final String SETTINGS_TZ_PREFIX   = "SETTINGS_TZ_";
```

---

## 8. CallbackResult Value Object

Callbacks can return either a text+keyboard edit, or a full `SendMessage` (when a new message is needed instead of editing).
Wrap both cases in a small package-private value object.

```java
final class CallbackResult {
    final String text;
    final InlineKeyboardMarkup markup;
    final SendMessage sendMessage;   // non-null = send new message instead of editing
}
```

The bot's `consume()` method decides what Telegram API call to make based on which field is set.

---

## 9. Internationalisation — Enum + Constant Keys + Properties Files

Use a `Lang` enum (not raw strings) to represent supported languages.
Each enum value carries a `Locale` for date/time formatting.

```java
enum Lang { EN, RU;
    public Locale locale() { return this == EN ? Locale.ENGLISH : Locale.of("ru"); }
}
```

All message keys are `public static final String` constants on a `Messages` class.
Never use raw string literals for message keys in handlers.

```java
Messages.get(lang, Messages.FRIEND_ADDED, name)   // ✅
Messages.get(lang, "friend_added", name)           // ❌ — typo-prone, no compile-time check
```

Message files: `i18n/messages_en.properties`, `messages_ru.properties`.
Format: `key=value with %s placeholders`. Newlines in values use `\n` literal, replaced at read time.

Configure `MessageSource` explicitly with `setFallbackToSystemLocale(false)` to prevent silent fallback to the server locale.

---

## 10. Bilingual Enums — Embed Labels Directly

When an enum value needs a display label in multiple languages, embed the labels on the enum itself.

```java
enum Relationship {
    FRIEND("👫 Friend", "👫 Друг"),
    PARTNER("💑 Partner", "💑 Партнёр");

    public final String labelEn, labelRu;

    public String label(Lang lang) {
        return lang == Lang.EN ? labelEn : labelRu;
    }
}
```

No external lookup table. The enum is self-describing. Adding a new relationship = one enum constant.

---

## 11. Rich Domain Entities

Entities carry domain logic, not just data.

```java
class Friend {
    public LocalDate nextBirthday(LocalDate from) { ... }  // handles leap year Feb 29
    public int getNextAge(LocalDate from) { ... }
    public String getZodiac() { ... }
}
```

This keeps service and handler code free of date arithmetic.
The entity is the right place for logic that is purely about that entity's data.

Leap year handling belongs in `nextBirthday()` — one place, tested once.

---

## 12. Service Layer — Thin, Focused, No Presentation Logic

`FriendService` is a thin facade over the repository. It:
- Enforces business rules (friend cap, case-insensitive name uniqueness).
- Provides sorted/filtered views (`getFriendsSortedByDayMonth` — sorts by day/month using a fixed leap year so Feb 29 sorts correctly).
- Exposes bulk queries (`getFriendsByUserIds`) to avoid N+1 in the scheduler.
- Uses a private `update()` helper with a `Consumer<Friend>` to avoid repeating find-mutate-save.

It does **not** contain presentation logic (formatting, i18n). That belongs in handlers.

---

## 13. Repository — Derived Query Methods + Custom Aggregation

Use Spring Data derived query method names for simple queries. Use `@Aggregation` for distinct/group operations.

```java
interface FriendMongoRepository extends MongoRepository<Friend, String> {
    List<Friend> findByTelegramUserId(long telegramUserId);
    List<Friend> findByTelegramUserIdIn(List<Long> ids);          // bulk load for scheduler
    Optional<Friend> findByTelegramUserIdAndNameIgnoreCase(...);  // case-insensitive lookup
    boolean existsByTelegramUserIdAndNameIgnoreCase(...);
    void deleteByTelegramUserIdAndNameIgnoreCase(...);
    void deleteByTelegramUserId(long telegramUserId);             // account deletion

    @Aggregation("{ $group: { _id: '$telegramUserId' } }")
    List<Long> findDistinctTelegramUserIds();                     // for scheduler: who has friends
}
```

Index `telegramUserId` on the entity (`@Indexed`) — every query filters by it.

---

## 14. MessageBuilder — Centralise Message Construction

A single `MessageBuilder` utility class owns:
- `SendMessage` factory methods (`html(chatId, text)`, `html(chatId, text, markup)`).
- The shared `DateTimeFormatter`.
- The reply-keyboard label → command mapping (`LABEL_TO_COMMAND`).
- The main menu reply keyboard layout.

No handler constructs `SendMessage` directly. They call `MessageBuilder.html(...)`.
All messages use `parseMode("HTML")` — never Markdown (escaping is fragile).

---

## 15. Keyboard Builders as Pure Static Functions

Keyboard construction is pure: given inputs, return an `InlineKeyboardMarkup`. No side effects, no state.

```java
// DatePickerKeyboard — pure static methods
static InlineKeyboardMarkup yearKeyboard(int pageOffset, Lang lang) { ... }
static InlineKeyboardMarkup monthKeyboard(Lang lang, int yearPageOffset) { ... }
static InlineKeyboardMarkup dayKeyboard(int year, int month, Lang lang) { ... }
```

Keyboard prefix constants live on the same class as the keyboard builder.
This co-locates the data contract (what strings are sent) with the UI that produces them.

Active selection is shown with a `✅` prefix on the button label — no separate "selected" state needed.

---

## 16. Pagination Pattern

Any list that can grow unbounded (friends, search results) must be paginated.
Use a consistent `PAGE_SIZE` constant and `◀ ▶` navigation buttons with page index in callback data.

```java
public static final String LIST_PAGE_PREFIX = "LIST_PAGE_";
public static final int PAGE_SIZE = 10;
```

The keyboard builder receives `(lang, sortMode, page, totalCount)` and adds nav buttons only when `totalPages > 1`.
Page index is embedded in callback data: `LIST_PAGE_DATE_2`, `REMOVE_PAGE_1`.

---

## 17. Confirmation Flow Pattern

Destructive actions (remove friend, delete account) always require a two-step confirmation via inline keyboard.

1. Handler sends a message with Yes/No inline buttons.
2. `CONFIRM_*` callback performs the action. `CANCEL_*` callback cancels and clears state.
3. State is set to `WAITING_FOR_*_CONFIRM` so the user can't accidentally trigger it via text.

```java
// Callback data constants on the handler that owns the action
public static final String CONFIRM_DELETE = "CONFIRM_DELETE_ACCOUNT";
public static final String CANCEL_DELETE  = "CANCEL_DELETE_ACCOUNT";
```

---

## 18. Bulk Input — Parse, Validate, Report

For bulk operations (CSV import), separate parsing from persistence:

1. `BulkAddParser.parse()` — pure function, takes lines + existing names, returns `ParseResult(valid, errors)`.
2. Handler applies the cap check, persists valid entries, clears state.
3. Response shows both success count and per-line error messages.

```java
record ParseResult(List<Friend> valid, List<String> errors) {}
```

Skip the CSV header row automatically by checking if the first row contains "name".
Support both pasted text and uploaded `.csv` file via the same state handler — check `hasDocument()` vs `hasText()`.

---

## 19. Export — Stream to File, Send as Document

For data export, build the file content in memory as a `ByteArrayInputStream` and send via `SendDocument`.
Never write to disk. Handle CSV quoting correctly (names containing commas or quotes).

```java
telegramClient.execute(SendDocument.builder()
    .chatId(chatId)
    .document(new InputFile(new ByteArrayInputStream(csv.getBytes(UTF_8)), "friends.csv"))
    .caption(Messages.get(lang, Messages.EXPORT_CAPTION))
    .build());
```

Return `null` from `handle()` after sending the document — the bot's `consume()` skips null responses.

---

## 20. Notification Scheduler — Interface Segregation + Bulk Loading

The scheduler (`BirthdayReminder`) depends on a `NotificationSender` interface, not on the bot directly.

```java
interface NotificationSender {
    void send(long telegramUserId, String text);
}
```

`BirthdayBot` implements `NotificationSender`. In tests, mock it freely.

Scheduler design:
- Runs every hour via cron (`0 0 * * * *`).
- Loads all user preferences and friends in **two bulk queries** — not N+1.
- Checks each user's timezone and notify hour before sending.
- Tracks `lastNotifiedDate` to prevent duplicate notifications on the same day.
- Notification offsets (today=0, tomorrow=1, week=7) are a `Map<Long, String>` — adding a new offset is one line.
- Per-user failures are caught individually so one bad user never blocks others.
- Uses MDC (`MDC.put("userId", ...)`) inside the per-user loop for log correlation.
- Also runs on `@PostConstruct` to catch up on missed notifications after a restart.

---

## 21. Optional Feature Toggle — Null Object Pattern

For optional integrations (Kafka metrics, external APIs), define an interface and provide two implementations:
a real one and a no-op one. Use `@ConditionalOnProperty` to activate the right one.

```java
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
class FestivaMetricsSender implements MetricsSender { ... }

@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
class NoOpMetricsSender implements MetricsSender {
    public void sendMetrics(Update update, String status, long ms) {}
}
```

Callers never check if the feature is enabled. They just call the interface.
The no-op is the default — the feature is opt-in.

---

## 22. Metrics — Capture Every Update

Wrap the entire `consume()` body in try/catch and send metrics for both success and failure.

```java
long start = System.currentTimeMillis();
try {
    // dispatch...
    metricsSender.sendMetrics(update, "SUCCESS", System.currentTimeMillis() - start);
} catch (Exception e) {
    metricsSender.sendMetrics(update, "ERROR", System.currentTimeMillis() - start);
    log.error("bot.update.failed: ...", e);
}
```

Metric payload includes: timestamp, userId, userName (sanitised), command/callback (anonymised), status, processingTimeMs.
Sanitise all user-provided strings before sending to external systems (strip quotes, newlines).

---

## 23. Logging Convention — Structured Key=Value

Log format: `"domain.action: key=value, key=value"` — no prose sentences.

```java
log.info("reminder.check.done: userCount={}, notifiedCount={}", userIds.size(), count);
log.debug("router.command: userId={}, command={}", userId, command);
log.error("bot.update.failed: updateId={}, type={}, message={}", id, type, e.getMessage(), e);
```

Level rules:
- `DEBUG` — per-user actions (command dispatched, friend added, notification sent, session cancelled).
- `INFO` — system lifecycle events (bot started, commands registered, scheduler ran).
- `WARN` — bad user input or recoverable issues (invalid timezone, unknown callback data, oversized file).
- `ERROR` — unexpected failures (Telegram API error, serialisation failure).

Never log PII (names, birthdates) at INFO or above.
Use MDC in loops to correlate log lines per user without passing userId to every method.

---

## 24. Testing Strategy — Three Layers

**Unit tests** (no Spring, no DB):
- Test handlers, router, state service, entity logic, parser in isolation.
- Use Mockito to mock dependencies.
- Fast. Run on every save.

**Integration tests** (Spring context + real DB):
- Extend a shared `IntegrationTestBase` that starts a Testcontainers MongoDB instance once for the whole suite.
- Mock only the bot (to avoid Telegram API calls at startup).
- Test service + repository together against a real database.

```java
@SpringBootTest @ActiveProfiles("test") @Testcontainers
public abstract class IntegrationTestBase {
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @MockitoBean BirthdayBot birthdayBot;  // prevent real bot startup

    @BeforeAll static void startContainers() { MONGO.start(); }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
```

The container starts once and is reused across all integration test classes.

**Test profile** (`application-test.yml`):
- Excludes the Telegram bot auto-configuration: `exclude: TelegramBotStarterConfiguration`.
- Sets `kafka.enabled: false` to activate the no-op metrics sender.
- Uses a separate test database name.

**Test naming:** `@DisplayName` on every test, written as a sentence describing the behaviour.
```java
@DisplayName("birthday today → notification contains friend's name")
```

---

## 25. Configuration — Environment Variables + Defaults

All secrets and environment-specific values come from environment variables.
Provide safe defaults for local development in `application.yml`.

```yaml
spring.data.mongodb.uri: ${MONGO_URI:mongodb://localhost:27017}
telegram.bot.token: ${TELEGRAM_BOT_TOKEN}       # no default — must be set
kafka.enabled: ${APP_KAFKA_ENABLED:false}        # optional feature, off by default
```

Maintain three env files:
- `.env` — local development defaults (committed, no real secrets).
- `.env.prod` — production template (committed, values filled in on the server).
- `.gitignore` — never commit actual secrets.

---

## 26. Docker — Multi-Stage Build with Layer Caching + CDS

Four stages:
1. **build** — Maven compiles and packages the fat JAR (with `-DskipTests`).
2. **extract** — splits the fat JAR into layers (dependencies / loader / snapshot-deps / application) for Docker cache efficiency. Dependencies layer rarely changes and is cached.
3. **cds-train** — generates a JVM Class Data Sharing archive for faster startup (`-XX:ArchiveClassesAtExit`).
4. **runtime** — minimal JRE Alpine image, copies layers + CDS archive, sets JVM flags.

JVM flags for containers:
```
-XX:+UseContainerSupport        # respect cgroup memory limits
-XX:MaxRAMPercentage=60.0       # leave headroom for the OS
-XX:+ExitOnOutOfMemoryError     # fail fast, let the orchestrator restart
-XX:+UseG1GC                    # predictable GC pauses
-XX:G1HeapRegionSize=4m
-XX:+UseStringDeduplication
-XX:SharedArchiveFile=app-cds.jsa
```

A separate `Dockerfile.deps` pre-warms the Maven dependency cache for CI speed:
```dockerfile
FROM maven:3.9-eclipse-temurin-25-alpine
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
```

---

## 27. Docker Compose — Profile-Based Service Activation

Separate infrastructure from the application using Docker Compose profiles.

```yaml
services:
  mongo:                    # always available — no profile
    image: mongo:8
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]

  bot:
    profiles: [bot]         # only started with --profile bot
    depends_on:
      mongo:
        condition: service_healthy   # waits for healthy DB before starting
    restart: unless-stopped
```

Development: `docker compose up -d mongo` → run app from IDE.
Full Docker: `docker compose --profile bot up -d --build`.
Production: separate `docker-compose.prod.yml` with `env_file: .env.prod` and MongoDB Atlas URI via `SPRING_APPLICATION_JSON`.

Health check on the bot uses `wget -qO- http://localhost:8080/health` — Spring Actuator exposes this endpoint.

---

## 28. Bot Lifecycle — @PostConstruct / @PreDestroy

Register the bot and set commands in `@PostConstruct`. Close the polling application in `@PreDestroy`.

```java
@PostConstruct
public void start() {
    botsApplication = new TelegramBotsLongPollingApplication();
    botsApplication.registerBot(botToken, this);
    telegramClient.execute(SetMyCommands.builder().commands(...).build());
}

@PreDestroy
public void stop() throws Exception {
    if (botsApplication != null) botsApplication.close();
}
```

`SetMyCommands` registers the command list in the Telegram UI (the `/` menu). Failures here are logged but don't crash the bot.

---

## 29. TelegramClient as a Spring Bean

Inject `TelegramClient` as a Spring bean, not constructed inline. This makes it mockable in tests.

```java
@Configuration
public class TelegramClientConfig {
    @Bean
    public TelegramClient telegramClient(@Value("${telegram.bot.token}") String token) {
        return new OkHttpTelegramClient(token);
    }
}
```

---

## 30. Virtual Threads

Enable Spring's virtual thread support for free concurrency without thread pool tuning.

```yaml
spring.threads.virtual.enabled: true
```

Each incoming update is handled on a virtual thread. Blocking I/O (DB, Telegram API) does not waste OS threads.

---

## 31. Shared Test Support Base for i18n — MessagesTestSupport

Unit tests that assert on message text need `Messages` initialised without a full Spring context.
Extract a `MessagesTestSupport` base class that initialises `MessageSource` once via `@BeforeAll`.

```java
public abstract class MessagesTestSupport {
    @BeforeAll
    static void initMessages() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        Messages.initForTest(source);
    }
}
```

Unit tests that need i18n extend `MessagesTestSupport`. Integration tests extend `IntegrationTestBase`.
Never duplicate the `MessageSource` setup across test classes.

---

## 32. End-to-End Flow Tests — Commands + Callbacks Together

Beyond unit and integration tests, write flow tests that exercise a full user journey:
command → state transition → callback → persistence — all in one test against a real DB.

```java
@Test
@DisplayName("/add → name → year/month/day callbacks → persists friend and confirms")
void addFriend_persistsAndConfirms() {
    commandRouter.route(update(1L, "/add"));
    commandRouter.route(update(1L, "Alice"));
    callbackQueryHandler.handle(callback(1L, "DATE_YEAR_1990"));
    callbackQueryHandler.handle(callback(1L, "DATE_MONTH_6"));
    callbackQueryHandler.handle(callback(1L, "DATE_DAY_15"));
    callbackQueryHandler.handle(callback(1L, "RELATIONSHIP_FRIEND"));

    assertThat(friendService.getFriends(1L).getFirst().getName()).isEqualTo("Alice");
}
```

These tests catch integration bugs that unit tests miss: wrong state transitions, missing `clearState()` calls, callback prefix mismatches.

---

## 33. Parameterized Tests for Entity Logic and i18n Coverage

Use `@ParameterizedTest` with `@CsvSource` for boundary-heavy entity logic (date calculations, zodiac signs).
Use `@EnumSource` to assert that every language resolves every message key.

```java
// Entity boundary test
@ParameterizedTest(name = "{0}")
@CsvSource({
    "birthday already passed → rolls to next year, 1990-01-01, 2024-06-01, 2025-01-01",
    "birthday still ahead   → stays same year,    1990-12-31, 2024-06-01, 2024-12-31",
    "birthday is today      → returns today,       1990-06-01, 2024-06-01, 2024-06-01",
})
void nextBirthday(String label, LocalDate birth, LocalDate from, LocalDate expected) { ... }

// i18n coverage test
@ParameterizedTest
@EnumSource(Lang.class)
void allKeysResolveForBothLangs(Lang lang) {
    assertThat(Messages.get(lang, Messages.WELCOME)).isNotBlank().isNotEqualTo(Messages.WELCOME);
    // key == value means the key was missing from the properties file
}
```

The `isNotEqualTo(key)` assertion catches missing translations — `Messages.get()` returns the key itself as fallback.

---

## 34. Narrow Exception Catching

Never catch broad `Exception` or `Throwable`. Catch only what you expect and can handle.

```java
// ✅
} catch (TelegramApiException | RuntimeException e) {

// ❌
} catch (Exception e) {
```

For `InterruptedException` specifically, always restore the interrupt flag:
```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restore interrupt status
    log.warn("task.interrupted", e);
}
```

Broad catches hide bugs and make it impossible to reason about what can fail.

---

## 35. Context-Aware /cancel

`/cancel` behaves differently depending on whether the user is in an active flow or idle.
Return a different message for each case — don't silently do nothing.

```java
boolean active = userStateService.getState(userId) != BotState.IDLE;
if (active) userStateService.clearState(userId);
String key = active ? Messages.CANCEL_ACTIVE : Messages.CANCEL_IDLE;
return MessageBuilder.html(chatId, Messages.get(lang, key), MessageBuilder.mainMenu());
```

Always attach the main menu keyboard on `/cancel` — it re-orients the user.

---

## 36. Persistent Reply Keyboard on /start

`/start` always attaches a `ReplyKeyboardMarkup` with the main commands.
This gives users a persistent UI affordance — they never need to remember or type commands.

```java
return MessageBuilder.html(chatId, welcome, MessageBuilder.mainMenu());
```

The reply keyboard is `resizeKeyboard(true)` so it doesn't take up excessive screen space.
The label → command map in `MessageBuilder.LABEL_TO_COMMAND` maps button text back to commands in the router.

---

## 37. Active State Reflected in UI

When showing a selection UI (language picker, settings, sort toggle), mark the currently active option with `✅`.
This is done at render time — no separate "selected" state is stored.

```java
// Language picker — mark current language
InlineKeyboardButton.builder()
    .text((lang == Lang.EN ? "✅ " : "") + Messages.get(lang, Messages.LANG_EN_BTN))
    .callbackData("LANG_" + Lang.EN.name())
    .build()

// Settings hour — mark active hour
String label = (hour == activeHour ? "✅ " : "") + String.format("%02d:00", hour);
```

Apply this consistently: sort buttons, timezone buttons, notification hour buttons, language buttons.

---

## 38. Contextual Data in Keyboard Button Labels

Embed live data directly in button labels — don't make users tap to discover it.

```java
// Month keyboard: show friend count per month and pin current month
long count = countByMonth.getOrDefault(month, 0L);
String label = (month == currentMonth ? "📍 " : "") + monthName + (count > 0 ? " (" + count + ")" : "");
```

This pattern applies broadly:
- Month picker: `📍 Jun (3)` — pinned current month + friend count
- Sort buttons: `✅ 📅 By date` — active sort mode
- Settings: `✅ 09:00` — active notification hour
- Language: `✅ 🇬🇧 English` — current language

The keyboard is the UI. Make it informative, not just navigational.

---

## 39. Reusable buildText() on Handlers

When a handler's response text needs to be regenerated from a callback (e.g. after changing sort order or filter),
extract `buildText()` as a public method on the handler so the callback handler can call it directly.

```java
// ListCommandHandler
public String buildText(List<Friend> friends, Lang lang, boolean byDate, int page) { ... }
public InlineKeyboardMarkup keyboard(Lang lang, boolean byDate, int page, int total) { ... }

// CallbackQueryHandler reuses it
return new CallbackResult(
    listHandler.buildText(friends, lang, byDate, page),
    listHandler.keyboard(lang, byDate, page, friends.size()));
```

This avoids duplicating formatting logic between the command handler and the callback handler.
Applies to: list sort/page, upcoming filter, remove/edit page navigation.

---

## 40. Local record for Intermediate Computation

When a handler needs to compute multiple derived values per item (e.g. next birthday + days until),
use a local `record` to avoid computing the same value multiple times.

```java
// ✅ — compute once, use three times
record Entry(Friend friend, LocalDate next, long days) {}

List<Entry> upcoming = friends.stream()
    .map(f -> { LocalDate next = f.nextBirthday(today); return new Entry(f, next, ChronoUnit.DAYS.between(today, next)); })
    .filter(e -> e.days() <= daysLimit)
    .sorted(Comparator.comparing(Entry::next))
    .toList();

// ❌ — nextBirthday() called 3× per friend
friends.stream()
    .filter(f -> ChronoUnit.DAYS.between(today, f.nextBirthday(today)) <= daysLimit)
    .sorted(Comparator.comparing(f -> f.nextBirthday(today)))
    ...
```

Local records are also useful in `StatsCommandHandler` to pair a friend with their computed days-until value for `min()`.

---

## 41. .dockerignore — Keep the Image Clean

A precise `.dockerignore` prevents secrets, test sources, docs, and IDE files from entering the image.

```
.env*                  # secrets — injected at runtime, never baked in
src/test/              # test sources not needed in production image
target/                # build output — rebuilt inside Docker
docs/ *.md             # documentation
.idea/ .vscode/        # IDE files
docker-compose*.yml    # compose files not needed inside the image
.git/                  # version control history
```

This also speeds up the Docker build context transfer significantly on large projects.

---

## 42. Live Bot Smoke Test Script

Beyond unit and integration tests, maintain a shell script that sends real Telegram messages to a test bot and asserts on replies using `getUpdates`.

```bash
send() { curl -s "${BASE}/sendMessage" --data-urlencode "chat_id=${CHAT_ID}" --data-urlencode "text=$1" }
assert() { actual=$(bot_reply_after "$2"); echo "$actual" | grep -qiE "$3" && ((PASS++)) || ((FAIL++)); }

assert "/start returns welcome"   "$(send /start)"      "birthday|welcome"
assert "/add prompts for name"    "$(send /add)"         "name"
assert "duplicate rejected"       "$(send ExistingName)" "already|exist"
assert "unknown command fallback" "$(send /nope)"        "unknown"
```

This catches regressions that no unit or integration test can — actual Telegram API behaviour, message formatting, bot command registration. Run it against a dedicated test bot token before every release.

---

## 43. In-Memory State is Ephemeral — Document It

`ConcurrentHashMap`-based session state is lost on restart. This is an explicit, documented trade-off:
- Acceptable for single-instance deployments.
- Users mid-flow simply restart their command after a redeploy.
- For multi-instance or crash-recovery requirements, migrate to Redis (Spring Data Redis).

Document this limitation in the codebase (changelog, roadmap, or ADR). Don't let it be a surprise.

---

## Summary Checklist

| # | Practice |
|---|---|
| 1 | Package by feature with sub-packages (api/, entity/, repository/) |
| 2 | Command handler = strategy + auto-registry, no if/else chains |
| 3 | Stateful handlers extend the base interface, declare their states |
| 4 | Fixed, documented routing priority order |
| 5 | FSM enum for conversation state |
| 6 | Single session object per user in ConcurrentHashMap |
| 7 | Callback routing by prefix, constants on owning class, grouped dispatch |
| 8 | CallbackResult value object for dual return types |
| 9 | Lang enum + constant message keys, explicit MessageSource config |
| 10 | Bilingual enums with embedded labels |
| 11 | Rich domain entities with business logic (nextBirthday, zodiac, leap year) |
| 12 | Thin service layer, no presentation logic, Consumer<T> update helper |
| 13 | Derived query methods + @Aggregation, @Indexed on partition key |
| 14 | MessageBuilder centralises SendMessage construction, HTML parse mode |
| 15 | Keyboard builders as pure static functions, ✅ prefix for active state |
| 16 | Pagination with PAGE_SIZE constant and ◀ ▶ nav in callback data |
| 17 | Two-step confirmation for destructive actions |
| 18 | Bulk input: pure parser → ParseResult record → cap check → persist |
| 19 | Export as ByteArrayInputStream → SendDocument, return null after send |
| 20 | NotificationSender interface, bulk DB load, MDC, @PostConstruct catch-up |
| 21 | Null Object pattern for optional features, opt-in via property |
| 22 | Metrics on every update (success + error), sanitise user strings |
| 23 | Structured domain.action: key=value logs, level rules, no PII above DEBUG |
| 24 | Three-layer tests: unit / integration / shared Testcontainers base |
| 25 | Env vars + defaults, three env files, never commit secrets |
| 26 | Multi-stage Docker build: build → extract → cds-train → runtime |
| 27 | Docker Compose profiles, depends_on with health condition, prod compose |
| 28 | @PostConstruct / @PreDestroy for bot lifecycle, SetMyCommands on start |
| 29 | TelegramClient as a Spring bean for testability |
| 30 | Virtual threads enabled |
| 31 | MessagesTestSupport base class — i18n init once, no Spring context needed |
| 32 | End-to-end flow tests: command → state → callback → DB assertion |
| 33 | @ParameterizedTest with @CsvSource for entity boundaries, @EnumSource for i18n coverage |
| 34 | Narrow exception catching, Thread.currentThread().interrupt() for InterruptedException |
| 35 | Context-aware /cancel: different message for active vs idle, always re-attach menu |
| 36 | Persistent reply keyboard on /start, label→command map in router |
| 37 | Active state reflected in UI with ✅ prefix, computed at render time |
| 38 | Contextual data in keyboard button labels (count badges, pin emoji, active checkmark) |
| 39 | Reusable buildText() on handlers so callbacks can regenerate text without duplication |
| 40 | Local record for intermediate computation — avoid repeated method calls in streams |
| 41 | .dockerignore — exclude secrets, test sources, docs, IDE files, compose files |
| 42 | Live bot smoke test script — real Telegram API assertions before every release |
| 43 | In-memory state is ephemeral — document the trade-off, plan Redis migration path |
