# Festiva — UX Improvements Backlog

Prioritized by user impact. Each item references the affected file(s).

---

## 🔴 High Priority

### 1. Inline keyboard for `/add` — date picker instead of typed input
**Problem:** User must type `15.03.1990` manually. Most users get the format wrong on first try.  
**Fix:** After entering a name, show a year → month → day inline keyboard flow.  
**Files:** `AddFriendCommandHandler`, `CallbackQueryHandler`, `BotState`

---

### 2. Confirmation step before removing a friend
**Problem:** Tapping a name in `/remove` instantly deletes — no undo, no confirmation.  
**Fix:** After tapping a name, show "Are you sure? ✅ Yes / ❌ No" inline buttons.  
**Files:** `CallbackQueryHandler` (`handleRemove`), `Messages`

---

### 3. Notification messages include age
**Problem:** `"🎂 Today is your friend Anna's birthday!"` — no age context.  
**Fix:** Include the age in all 3 notification messages (today / tomorrow / week).  
**Files:** `BirthdayReminder`, `Messages` (`NOTIFY_TODAY`, `NOTIFY_TOMORROW`, `NOTIFY_WEEK`)

---

### 4. `/start` shows a persistent reply keyboard
**Problem:** Users must remember or type commands. No persistent UI affordance.  
**Fix:** Attach a `ReplyKeyboardMarkup` with the main commands on `/start`.  
**Files:** `StartCommandHandler`, `MessageBuilder`

---

## 🟡 Medium Priority

### 5. `/list` shows days until next birthday
**Problem:** Shows age info but not "how many days until their birthday" — the most useful info.  
**Fix:** Add `(in X days)` or `(today! 🎂)` suffix per friend.  
**Files:** `ListCommandHandler`

---

### 6. `/upcomingbirthdays` shows the actual date
**Problem:** Shows `(turns 30, days left — 5)` but not the actual `DD.MM` date.  
**Fix:** Add the date to each entry.  
**Files:** `UpcomingBirthdaysCommandHandler`, `Messages` (`UPCOMING_TURNS`)

---

### 7. Duplicate name check is case-insensitive bug
**Problem:** `"anna"` and `"Anna"` are treated as different friends — real data integrity bug.  
**Fix:** Normalize to lowercase for existence check in `friendExists` and `deleteFriend`.  
**Files:** `FriendService`, `FriendMongoRepository`

---

### 8. `/birthdays` month keyboard shows month names, not numbers
**Problem:** Keyboard shows `1 2 3 4 / 5 6 7 8 / 9 10 11 12` — not user-friendly.  
**Fix:** Show abbreviated localized month names (`Jan Feb Mar ...`).  
**Files:** `BirthdaysCommandHandler`
