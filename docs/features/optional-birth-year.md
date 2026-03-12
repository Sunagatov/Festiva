# Feature: Optional Birth Year

> **Status:** `Planned`
> **Related Commands:** `/add`, `/addmany`, `/edit`
> **Handlers:** `AddFriendCommandHandler.java`, `DatePickerCallbackHandler.java`, `EditFriendCommandHandler.java`

---

## 1. Overview

Allows users to add friends without specifying the birth year when the exact year is unknown.
The bot will still send birthday reminders on the correct date but will skip age and jubilee calculations.

---

## 2. User Stories

- As a user, I want to add a friend when I know their birthday (month + day) but not their birth year, so I can still get reminders.
- As a user, I want to see reminders that don't mention age when the birth year is unknown, so the message feels appropriate.
- As a user, I want to add the birth year later via `/edit` when I learn it.

---

## 3. Functional Requirements

1. During `/add` flow, year picker includes **"⏭ Skip year"** button.
2. If year is skipped, user proceeds directly to month picker.
3. Friend name remains **required** (unchanged from current behavior).
4. Birthdate is stored as:
   - `Integer birthYear` (nullable) - null when year is unknown
   - `int birthMonth` (1-12, always set)
   - `int birthDay` (1-31, always set)
5. Validation happens in constructor:
   - If year is provided: `LocalDate.of(year, month, day)` validates full date
   - If year is null: `MonthDay.of(month, day)` validates month+day only
6. Reminders for no-year friends:
   - Show name and date
   - Skip age mention
   - Skip jubilee detection
7. `/edit` allows adding a year to a no-year friend or removing a year from a full-date friend.
8. `/list`, `/birthdays`, `/today`, `/upcomingbirthdays` display no-year friends without age.
9. `/jubilee` excludes no-year friends (cannot calculate jubilee without year).
10. `/stats` excludes no-year friends from age-related calculations.
11. `/export` CSV year column is empty for no-year friends.
12. `/addmany` CSV import supports empty year to indicate unknown year.

---

## 4. Non-Functional Requirements

- **i18n:** All new messages must exist in `messages_en.properties` and `messages_ru.properties`.
- **Data migration:** Existing `LocalDate birthDate` field must be split into `Integer birthYear`, `int birthMonth`, `int birthDay`.
- **Backward compatibility:** All existing features continue to work with full-date friends.
- **Validation:** Constructor validates using `LocalDate.of()` or `MonthDay.of()` before storing. `@PostLoad` validates after MongoDB deserialization.
- **Leap year handling:** When year is null and Feb 29 is stored, `MonthDay.of(2, 29)` validates it. Reminders use Feb 28 in non-leap years.
- **MongoDB storage:** Simple integers (no custom converters needed).

---

## 5. Bot Flow Changes

### `/add` Flow with Year Skip

```
User sends: /add
  → Bot: "👤 Enter your friend's name:"

User sends: "Sarah"
  → Bot: "Select Sarah's birth year:" + year picker keyboard + [⏭ Skip year] button

User taps [⏭ Skip year]
  → Bot: "Select Sarah's birth month:" + month picker keyboard

User taps a month (e.g. June)
  → Bot: "Select Sarah's birth day:" + day picker keyboard

User taps a day (e.g. 15)
  → Bot: "Choose your relationship with Sarah:" + relationship keyboard

User taps a relationship or skips
  → Friend saved with birthYear = null, birthMonth = 6, birthDay = 15
  → Bot: "✅ Sarah added! (Birth year not specified)"
```

### `/add` Flow with Year Provided (unchanged)

```
User taps a year (e.g. 1995)
  → Bot: "Select Sarah's birth month:" + month picker keyboard
  → (rest of flow unchanged)
```

---

## 6. Display Format Changes

| Feature | With Year | Without Year |
|---|---|---|
| `/list` | "– **15.06.1990** ♊ *Alice* (Friend) turned 35 (in 2d)" | "– **15.06** ♊ *Sarah* (Friend) 🎂" |
| `/birthdays` | "– **15.06.1990** ♊ *Alice* (Friend) turned 35 (in 2d)" | "– **15.06** ♊ *Sarah* (Friend) 🎂" |
| `/today` | "– **15.06.1990** ♊ *Alice* (Friend) turned 35 🎂" | "– **15.06** ♊ *Sarah* (Friend) 🎂" |
| `/upcomingbirthdays` | "– **15.06.1990** ♊ *Alice* (Friend) (turns 35, days left — 2)" | "– **15.06** ♊ *Sarah* (Friend) (in 2d)" |
| `/jubilee` | "– **15.06.1990** ♊ *Alice* (Friend) (turns 40)" | (excluded) |
| Reminder (today) | "🎂 Today is **Alice** (Friend)'s birthday ♊ — turning **35 years old**!" | "🎂 Today is **Sarah** (Friend)'s birthday ♊!" |
| Reminder (tomorrow) | "🔔 Tomorrow is **Alice** (Friend)'s birthday ♊ — turning **35 years old**!" | "🔔 Tomorrow is **Sarah** (Friend)'s birthday ♊!" |
| Reminder (week) | "📅 In one week it's **Alice** (Friend)'s birthday ♊ — turning **35 years old**!" | "📅 In one week it's **Sarah** (Friend)'s birthday ♊!" |

---

## 7. Edge Cases & Alternative Flows

| Scenario | Behavior |
|---|---|
| Feb 29 without year in non-leap year | `nextBirthday()` returns Feb 28 |
| Sorting in `/list` | No-year friends sort by month-day only, intermixed with full-date friends |
| `/edit` add year to no-year friend | User selects date field → year picker appears → full date stored |
| `/edit` remove year from full-date friend | User selects date field → [⏭ Skip year] available → year set to null |
| `/export` CSV | Year column is empty for null birthYear |
| `/addmany` CSV with empty year | Friend imported with birthYear = null |
| `/stats` | No-year friends excluded from age calculations, included in total count |
| Zodiac calculation | Works correctly (only needs month + day) |
| `getAge()` called on no-year friend | Throws IllegalStateException with clear message |

---

## 8. State Transitions

No new states required. Existing states handle both flows:

| State | Behavior Change |
|---|---|
| `WAITING_FOR_ADD_FRIEND_DATE` | Year picker now includes [⏭ Skip year] button |
| (all other states) | Unchanged |

---

## 9. Data Model Changes

### Current `Friend` Entity

```java
@Document(collection = "friends")
public class Friend {
    private String id;
    private long telegramUserId;
    private String name;
    private LocalDate birthDate;  // ← Always full date
    private Relationship relationship;
    private boolean notifyEnabled;
    
    public int getAge(LocalDate on) {
        return Period.between(birthDate, on).getYears();
    }
    
    public LocalDate nextBirthday(LocalDate from) {
        LocalDate next = birthDate.withYear(from.getYear());
        if (next.isBefore(from)) next = next.plusYears(1);
        // ... leap year handling
        return next;
    }
    
    public int getNextAge(LocalDate from) {
        return nextBirthday(from).getYear() - birthDate.getYear();
    }
    
    public String getZodiac() {
        int m = birthDate.getMonthValue(), d = birthDate.getDayOfMonth();
        // ...
    }
}
```

### Proposed `Friend` Entity

```java
@Document(collection = "friends")
public class Friend {
    private String id;
    private long telegramUserId;
    private String name;
    
    // Store as simple integers (MongoDB-friendly)
    private Integer birthYear;    // ← Nullable (null = year unknown)
    private int birthMonth;       // ← 1-12 (always set)
    private int birthDay;         // ← 1-31 (always set)
    
    private Relationship relationship;
    private boolean notifyEnabled;
    
    // Constructor with validation BEFORE MongoDB
    public Friend(String name, LocalDate birthDate) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
    }
    
    public Friend(String name, Integer year, int month, int day) {
        this.name = name;
        
        // VALIDATE using Java's built-in types
        if (year != null) {
            LocalDate.of(year, month, day);  // Validates full date (throws if invalid)
        } else {
            MonthDay.of(month, day);  // Validates month+day only (throws if invalid)
        }
        
        // Only store if validation passed
        this.birthYear = year;
        this.birthMonth = month;
        this.birthDay = day;
    }
    
    // Safety net: validate after MongoDB deserialization
    @PostLoad
    private void validateAfterLoad() {
        try {
            if (birthYear != null) {
                LocalDate.of(birthYear, birthMonth, birthDay);
            } else {
                MonthDay.of(birthMonth, birthDay);
            }
        } catch (DateTimeException e) {
            throw new IllegalStateException("Invalid birth date loaded from database for " + name + ": " + e.getMessage());
        }
    }
    
    // Helper methods
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
    
    // Updated methods
    public int getAge(LocalDate on) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return Period.between(getBirthDate(), on).getYears();
    }
    
    public LocalDate nextBirthday(LocalDate from) {
        // Try to create the birthday in the current year
        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            // Feb 29 in non-leap year → use Feb 28
            next = LocalDate.of(from.getYear(), 2, 28);
        }
        
        // If already passed this year, move to next year
        if (next.isBefore(from)) {
            try {
                next = LocalDate.of(from.getYear() + 1, birthMonth, birthDay);
            } catch (DateTimeException e) {
                next = LocalDate.of(from.getYear() + 1, 2, 28);
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
    
    public String getZodiac() {
        int m = birthMonth;
        int d = birthDay;
        int idx = d <= ZODIAC_ENDS[m - 1][1] ? m - 1 : m;
        return ZODIAC_SIGNS[idx];
    }
}
```

**Why this approach is optimal:**

1. ✅ **Validation before MongoDB** - Constructor validates using `LocalDate.of()` or `MonthDay.of()` before storing
2. ✅ **No custom converters** - Stores as simple integers in MongoDB
3. ✅ **Type safety** - Uses Java's built-in date validation
4. ✅ **No magic numbers** - `null` explicitly means "year unknown"
5. ✅ **Safety net** - `@PostLoad` validates data after deserialization from DB
6. ✅ **Clean API** - `getBirthMonthDay()` returns `MonthDay` for convenience

**Migration strategy:**
```javascript
// One-time migration script
db.friends.find().forEach(friend => {
    const date = friend.birthDate;
    db.friends.updateOne(
        { _id: friend._id },
        { 
            $set: {
                birthYear: date.year,
                birthMonth: date.month,
                birthDay: date.day
            },
            $unset: { birthDate: "" }
        }
    );
});
```

---

## 10. Acceptance Criteria

- [ ] Given year picker is shown, when [⏭ Skip year] is tapped, then month picker appears.
- [ ] Given month and day are selected without year, when friend is saved, then `birthYear` is null and `birthMonth`/`birthDay` are set.
- [ ] Given invalid date (e.g. Feb 30), when constructor is called, then DateTimeException is thrown.
- [ ] Given Feb 29 with null year, when constructor is called, then validation passes (MonthDay.of(2, 29) is valid).
- [ ] Given a no-year friend exists, when `/list` is called, then friend is shown without year and age.
- [ ] Given a no-year friend has a birthday today, when reminder fires, then message does not mention age.
- [ ] Given a no-year friend exists, when `/jubilee` is called, then friend is excluded from results.
- [ ] Given a no-year friend exists, when `/stats` is called, then friend is excluded from age calculations.
- [ ] Given a no-year friend exists, when `/export` is called, then CSV year column is empty.
- [ ] Given a CSV with empty year is imported, when `/addmany` completes, then friend is saved with `birthYear = null`.
- [ ] Given a no-year friend exists, when `/edit` date is selected, then user can add a year.
- [ ] Given a full-date friend exists, when `/edit` date is selected, then user can remove the year via [⏭ Skip year].
- [ ] Given Feb 29 without year, when non-leap year, then `nextBirthday()` returns Feb 28.
- [ ] Given invalid date in database, when `@PostLoad` runs, then IllegalStateException is thrown.
- [ ] Given `getAge()` is called on no-year friend, then IllegalStateException is thrown.
- [ ] Given `getBirthDate()` is called on no-year friend, then IllegalStateException is thrown.

---

## 11. Security & Privacy

No changes. Ownership scoping remains unchanged.

---

## 12. Metrics & Observability

| Event | Log Level | Key Fields |
|---|---|---|
| `friend.added.no_year` | `DEBUG` | `userId`, `name`, `monthDay` |
| `friend.year_added` | `DEBUG` | `userId`, `name`, `oldMonthDay`, `newBirthDate` |
| `friend.year_removed` | `DEBUG` | `userId`, `name`, `oldBirthDate`, `newMonthDay` |

---

## 13. New Messages

| Scenario | Message Key | EN Text | RU Text |
|---|---|---|---|
| Friend added without year | `friend_added_no_year` | "✅ %s added! (Birth year not specified)\n\nUse /list to view friends or /add to add another." | "✅ %s добавлен(а)! (Год рождения не указан)\n\nИспользуйте /list для просмотра или /add, чтобы добавить ещё." |
| Skip year button | `date_skip_year` | "⏭ Skip year" | "⏭ Пропустить год" |
| Reminder today (no year) | `notify_today_no_year` | "🎂 Today is **%s**%s's birthday %s!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>" | "🎂 Сегодня день рождения у **%s**%s %s!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>" |
| Reminder tomorrow (no year) | `notify_tomorrow_no_year` | "🔔 Tomorrow is **%s**%s's birthday %s!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>" | "🔔 Завтра день рождения у **%s**%s %s!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>" |
| Reminder week (no year) | `notify_week_no_year` | "📅 In one week it's **%s**%s's birthday %s!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>" | "📅 Через неделю день рождения у **%s**%s %s!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>" |
| CSV year column header | N/A (in code) | "Year" | "Год" |

---

## 14. Relationships to Other Features

**Must update:**
- `add-friend.md` — add year skip flow
- `edit-friend.md` — add/remove year flow
- `list.md` — display format for no-year friends
- `birthdays-by-month.md` — display format for no-year friends
- `today.md` — reminder message format
- `upcoming-birthdays.md` — display format
- `jubilee.md` — exclusion of no-year friends
- `birthday-reminders.md` — reminder message format
- `stats.md` — exclusion from age calculations
- `export.md` — CSV format with optional year
- `bulk-add.md` — CSV import with optional year

---

## 15. Out of Scope

- Allowing name to be optional (name remains required)
- Guessing or estimating birth year based on age hints
- Asking user if they want to add year later (user can use `/edit` anytime)

---

## 16. Open Questions

- [ ] **Leap year handling:** Feb 29 without year in non-leap year — remind on Feb 28 or Mar 1?
- [ ] **Sorting:** Should no-year friends appear intermixed with full-date friends by month-day, or grouped separately?
- [ ] **UI copy:** Should year picker button say "⏭ Skip year" or "❓ Year unknown"?
- [ ] **CSV import:** Should year column be optional or required-but-can-be-empty?

---

## 17. Testing Notes

**New test coverage needed:**
- Year skip button in date picker
- Friend saved with `birthMonthDay` only
- Display format in `/list`, `/birthdays`, `/today`, `/upcomingbirthdays`
- Exclusion from `/jubilee` and `/stats` age calculations
- CSV export with empty year column
- CSV import with missing year
- `/edit` add year to no-year friend
- `/edit` remove year from full-date friend
- Reminder message format for no-year friends

---

## 18. Implementation Phases

### Phase 1: Data Model & Core Logic
1. Replace `LocalDate birthDate` with `Integer birthYear`, `int birthMonth`, `int birthDay` in `Friend` entity
2. Add constructor that validates using `LocalDate.of()` or `MonthDay.of()` before storing
3. Add `@PostLoad` method to validate after MongoDB deserialization
4. Add `hasYear()` method: `return birthYear != null`
5. Add `getBirthMonthDay()` convenience method: `return MonthDay.of(birthMonth, birthDay)`
6. Add `getBirthDate()` method that throws if `!hasYear()`
7. Update `getAge()` and `getNextAge()` to throw IllegalStateException if `!hasYear()`
8. Update `nextBirthday()` to use `birthMonth` and `birthDay` with try-catch for Feb 29
9. Update `getZodiac()` to use `birthMonth` and `birthDay`
10. Write migration script to convert existing `birthDate` → `birthYear`, `birthMonth`, `birthDay`
11. Add unit tests for validation in constructor
12. Add unit tests for `Friend` methods with null `birthYear`
13. Test Feb 29 handling with and without year
14. Test `@PostLoad` validation with invalid data

### Phase 2: `/add` Flow
1. Add `DATE_SKIP_YEAR` constant to `DatePickerKeyboard`
2. Add [⏭ Skip year] button to `yearKeyboard()` method
3. Update `DatePickerCallbackHandler` to handle `DATE_SKIP_YEAR` callback
4. Update `AddFriendCommandHandler` to save with `birthYear = null` when skipped
5. Update `UserStateService` to track selected month/day separately from year
6. Add i18n messages: `friend_added_no_year`, `date_skip_year`

### Phase 3: Display Logic
1. Update `ListCommandHandler.appendFriend()` to check `hasYear()` and format accordingly
2. Update `BirthdaysCommandHandler` display logic
3. Update `TodayCommandHandler` display logic
4. Update `UpcomingBirthdaysCommandHandler` display logic
5. Update `JubileeCommandHandler` to filter `hasYear()` friends only
6. Update `StatsCommandHandler` to exclude no-year friends from age calculations

### Phase 4: Reminders
1. Update `BirthdayReminder.checkAndNotify()` to check `hasYear()`
2. Add new message templates: `notify_today_no_year`, `notify_tomorrow_no_year`, `notify_week_no_year`
3. Format reminder messages based on `hasYear()`

### Phase 5: Edit, Export, Import
1. Update `/edit` date flow to support [⏭ Skip year] button
2. Update `ExportCommandHandler` to leave year column empty for null `birthYear`
3. Update `BulkAddParser` to parse empty year as `birthYear = null`
4. Update CSV format to support optional year column (empty = no year)
5. Update CSV template to show example with and without year

### Phase 6: Testing & Documentation
1. Write unit tests for all updated methods
2. Write integration tests for `/add`, `/edit`, `/addmany` flows
3. Update all related feature docs (see section 14)
4. Update README.md with optional year feature

---

## 19. Changelog

| Date | Change |
|---|---|
| 2025-01-XX | Initial spec created |
