# Visual Text Audit — Festiva Bot Messages

Systematic audit of all bot-facing text for:
- Spacing / blank lines (`\n\n` vs `\n`)
- Missing `/cancel` hints in prompts
- Missing next-step hints in success/empty messages
- EN/RU parity (same structure, same tone)
- Inconsistent punctuation / capitalization
- Unicode escape vs literal character inconsistency (cosmetic, not a bug)

**Status:** ✅ OK | ⚠️ Issue | 🔧 Fixed

---

## Session 1 — Welcome / Menu / About

### `welcome`
- EN: `🎂 Welcome to Festiva!\n\n...`
- RU: `🎂 Добро пожаловать в Festiva!\n\n...`
- ✅ Structure identical. Double `\n\n` between paragraphs. No `/cancel` needed (not a prompt).

### `menu`
- EN uses `📖` (RU) vs `📖` — wait, EN uses `\ud83d\udcd6` = 📖, RU uses `📖` literal. Same emoji.
- EN uses `\u2014` (—), RU uses `—` literal. Same character.
- ✅ Structure identical. All commands listed in same order.

### `about`
- ✅ EN/RU parity. Same bullet structure. No `/cancel` needed.

**Session 1 result: ✅ No issues.**

---

## Session 2 — Add Friend flow

### `enter_name`
- EN: `👤 Enter your friend's name:\n<i>Tap /cancel at any time to stop.</i>`
- RU: `👤 Введите имя друга:\n<i>Нажмите /cancel в любой момент для отмены.</i>`
- ✅ Single `\n` before hint. Parity OK.

### `name_empty` / `name_too_long` / `name_exists`
- ✅ All have `/cancel` hint inline. Parity OK.

### `date_future_error`
- ✅ Has `/cancel` hint. Parity OK.

### `date_pick_year` / `date_pick_month` / `date_pick_day`
- EN: `Select <b>%s</b>'s birth year:\n<i>Tap /cancel at any time to stop.</i>`
- RU: `Выберите год рождения <b>%s</b>:\n<i>Нажмите /cancel в любой момент для отмены.</i>`
- ✅ Single `\n` before hint. Parity OK.

### `relationship_pick`
- EN: `Choose your relationship with <b>%s</b>:\n<i>Tap /cancel at any time to stop.</i>`
- RU: `Выберите отношение с <b>%s</b>:\n<i>Нажмите /cancel в любой момент для отмены.</i>`
- ✅ Parity OK.

### `friend_added`
- EN: `✅ %s added!`
- RU: `✅ %s добавлен(а) в список!`
- ⚠️ **Issue #1**: No next-step hint. After adding a friend the user is left with no guidance.
  - EN should be: `✅ %s added! Use /list to view friends or /add to add another.`
  - RU should be: `✅ %s добавлен(а)! Используйте /list для просмотра или /add, чтобы добавить ещё.`
- ⚠️ **Issue #2**: EN says "added!" RU says "добавлен(а) в список!" — minor parity gap ("в список" = "to the list", EN omits this).

### `friend_cap`
- ✅ Has next-step hint (`/remove`). Parity OK.

**Session 2 result: ⚠️ 2 issues on `friend_added`.**

---

## Session 3 — Remove flow

### `select_remove`
- EN: `Select a friend to remove. Tap /cancel to stop.`
- RU: `Выберите друга для удаления. Нажмите /cancel для отмены.`
- ✅ Has `/cancel` hint. Parity OK.

### `confirm_remove_ask`
- EN: `Remove <b>%s</b>? This cannot be undone.`
- RU: `Удалить <b>%s</b>? Это действие нельзя отменить.`
- ✅ Button-only state (Yes/No buttons shown). No `/cancel` hint needed here.

### `confirm_remove_cancel`
- EN: `❌ Removal cancelled. Your friend list is unchanged. Use /list to view friends or /remove to try again.`
- RU: `❌ Удаление отменено. Список друзей не изменён. Используйте /list для просмотра или /remove для повторной попытки.`
- ✅ Has next-step hints. Parity OK.

### `friend_removed`
- EN: `✅ <b>%s</b> removed! Use /add to add a new friend or /list to view your friends.`
- RU: `✅ <b>%s</b> удалён(а)! Используйте /add, чтобы добавить нового друга, или /list для просмотра списка.`
- ✅ Has next-step hints. Parity OK.

### `friend_not_found`
- EN: `❓ Friend "%s" not found. They may have already been removed.`
- RU: `❓ Друг «%s» не найден. Возможно, он уже был удалён.`
- ⚠️ **Issue #3**: No next-step hint. User is left hanging.
  - EN: add `Use /list to view your friends.`
  - RU: add `Используйте /list для просмотра списка.`
- ⚠️ **Issue #4**: EN uses `"` (straight quotes), RU uses `«»` (guillemets). EN should use `"…"` (curly) like other messages — EN already uses `\u201c%s\u201d` in `name_exists`. Inconsistency.

**Session 3 result: ⚠️ 2 issues.**

---

## Session 4 — Edit flow

### `edit_select`
- EN: `Select a friend to edit:`
- RU: `Выберите друга для редактирования:`
- ⚠️ **Issue #5**: No `/cancel` hint. This is a prompt that starts a multi-step flow.
  - EN: `Select a friend to edit. Tap /cancel to stop.`
  - RU: `Выберите друга для редактирования. Нажмите /cancel для отмены.`

### `edit_choose_field`
- EN: `Edit <b>%s</b> (%s) — what would you like to change? Tap /cancel to stop.`
- RU: `Редактировать <b>%s</b> (%s) — что изменить? Нажмите /cancel для отмены.`
- ✅ Has `/cancel` hint. Parity OK.

### `edit_enter_name`
- EN: `Enter a new name for <b>%s</b>:\n<i>Tap /cancel at any time to stop.</i>`
- RU: `Введите новое имя для <b>%s</b>:\n<i>Нажмите /cancel в любой момент для отмены.</i>`
- ✅ Parity OK.

### `edit_name_done` / `edit_date_done` / `edit_rel_done`
- EN: `✅ Name updated to <b>%s</b>! Use /edit to make more changes.`
- RU: `✅ Имя обновлено на <b>%s</b>! Используйте /edit для дальнейших изменений.`
- ✅ Has next-step hint. Parity OK.

### `edit_notify_toggled`
- EN: `🔔 Notifications for <b>%s</b>: <b>%s</b>. Use /edit to make more changes.`
- RU: `🔔 Уведомления для <b>%s</b>: <b>%s</b>. Используйте /edit для дальнейших изменений.`
- ✅ Parity OK.

**Session 4 result: ⚠️ 1 issue on `edit_select`.**

---

## Session 5 — List / Birthdays / Today / Upcoming / Jubilee

### `list_header`
- EN: `<b>Friends (current calendar year):</b>\n\n`
- RU: `<b>Список друзей (текущий календарный год):</b>\n\n`
- ✅ Double `\n\n` (header + blank line before list). OK.

### `list_upcoming_header` / `list_celebrated_header`
- EN: `<b>Coming up:</b>\n` / `\n<b>Already celebrated:</b>\n`
- RU: `<b>Предстоящие:</b>\n` / `\n<b>Уже отметили:</b>\n`
- ✅ `list_celebrated_header` has leading `\n` to create visual separation. Intentional. OK.

### `friends_empty`
- EN: `👥 <b>No friends yet.</b> Use /add to add your first one.`
- RU: `👥 <b>Друзей пока нет.</b> Используйте /add, чтобы добавить первого.`
- ✅ Has next-step hint. Parity OK.

### `birthdays_header`
- EN: `🎂 <b>Birthdays — %s</b>\n\n`
- RU: `🎂 <b>Дни рождения — %s</b>\n\n`
- ✅ Double `\n\n`. OK.

### `birthdays_none`
- EN: `No birthdays in <b>%s</b>. Try another month or use /add to add a friend.`
- RU: `В <b>%s</b> нет дней рождения. Попробуйте другой месяц или добавьте друга через /add.`
- ⚠️ **Issue #6**: EN missing emoji at start. RU has no emoji either — but compare to `today_none` (🎂) and `upcoming_none` (📅). Minor inconsistency.
- ✅ Has next-step hint. Parity OK otherwise.

### `birthdays_pick`
- EN: `<b>View birthdays</b>\n\nSelect a month:`
- RU: `<b>Просмотр дней рождения</b>\n\nВыберите месяц:`
- ✅ Double `\n\n` between title and instruction. OK. Button-only state, no `/cancel` needed.

### `today_header`
- EN: `<b>Today's birthdays:</b>\n\n`
- RU: `<b>Сегодняшние дни рождения:</b>\n\n`
- ✅ Double `\n\n`. OK.

### `today_none`
- EN: `🎂 <b>No birthdays today.</b> Check /upcomingbirthdays to see what's coming up, or /add to add a friend.`
- RU: `🎂 <b>Сегодня нет дней рождения.</b> Смотрите ближайшие через /upcomingbirthdays или добавьте друга через /add.`
- ✅ Has next-step hints. Parity OK.

### `today_hint`
- EN: `Use /list to see all friends or /upcomingbirthdays for what's coming next.`
- RU: `Используйте /list для просмотра всех друзей или /upcomingbirthdays для ближайших.`
- ✅ Parity OK.

### `upcoming_header`
- EN: `<b>Upcoming birthdays:</b>\n\n`
- RU: `<b>Ближайшие дни рождения:</b>\n\n`
- ✅ Double `\n\n`. OK.

### `upcoming_none`
- EN: `📅 <b>No birthdays in the next %d days.</b> Try a wider window above, or use /add to add a friend.`
- RU: `📅 <b>В ближайшие %d дней нет дней рождения.</b> Расширьте период выше или добавьте друга через /add.`
- ✅ Has next-step hints. Parity OK.

### `jubilee_header`
- EN: `<b>Milestone birthdays</b>\n\n`
- RU: `<b>Юбилейные дни рождения</b>\n\n`
- ✅ Double `\n\n`. OK.

### `jubilee_none`
- EN: `🏆 <b>No upcoming milestone birthdays.</b> Use /upcomingbirthdays to see all upcoming birthdays.`
- RU: `🏆 <b>Юбилейных дней рождения пока нет.</b> Используйте /upcomingbirthdays для просмотра всех ближайших.`
- ✅ Has next-step hint. Parity OK.

**Session 5 result: ⚠️ 1 minor issue on `birthdays_none` (missing emoji).**

---

## Session 6 — Search

### `search_prompt`
- EN: `🔍 Enter a name to search:\n<i>Tap /cancel at any time to stop.</i>`
- RU: `🔍 Введите имя для поиска:\n<i>Нажмите /cancel в любой момент для отмены.</i>`
- ✅ Has `/cancel` hint. Parity OK.

### `search_results`
- EN: `🔍 <b>Search results for "%s":</b>\n\n`
- RU: `🔍 <b>Результаты по «%s»:</b>\n\n`
- ✅ Double `\n\n` before list. Parity OK.

### `search_results_hint`
- EN: `Use /search to search again or /edit to edit a friend.`
- RU: `Используйте /search для нового поиска или /edit для редактирования.`
- ✅ Parity OK.

### `search_none`
- EN: `🔍 No friends found for "%s". Try a different name, or tap /cancel to stop.`
- RU: `🔍 Ничего не найдено по «%s». Попробуйте другое имя или нажмите /cancel.`
- ✅ Has `/cancel` hint. Parity OK.

### `search_too_long`
- ✅ Has `/cancel` hint. Parity OK.

**Session 6 result: ✅ No issues.**

---

## Session 7 — Bulk Add

### `bulk_add_choose`
- EN: `How would you like to add friends? Tap /cancel to stop.`
- RU: `Как вы хотите добавить друзей? Нажмите /cancel для отмены.`
- ✅ Has `/cancel` hint. Parity OK.

### `bulk_add_prompt`
- EN: `📋 <b>Bulk add friends</b>\n\nSend a text message or a <b>.csv file</b>...\n\nMax 50 entries. Tap /cancel to stop.`
- RU: `📋 <b>Массовое добавление друзей</b>\n\n...Максимум 50 записей. Нажмите /cancel для отмены.`
- ✅ Has `/cancel` hint. Double `\n\n` between sections. Parity OK.

### `bulk_add_success`
- EN: `✅ Added <b>%d</b> friend(s) successfully. Use /list to view your friends or /add to add one more.`
- RU: `✅ Добавлено <b>%d</b> друг(ов). Используйте /list для просмотра или /add для добавления ещё.`
- ✅ Has next-step hints. Parity OK.

### `bulk_add_errors`
- EN: `⚠️ Errors in <b>%d</b> line(s):\n%s\n\nFix the errors and use /addmany to try again.`
- RU: `⚠️ Ошибки в <b>%d</b> строках:\n%s\n\nИсправьте ошибки и используйте /addmany для повторной попытки.`
- ✅ `\n%s\n\n` — single `\n` after label, double `\n\n` before footer. Intentional. Parity OK.

### `bulk_add_empty` / `bulk_add_file_invalid` / `bulk_error_no_data`
- ✅ All have `/cancel` hint. Parity OK.

### `bulk_add_csv_caption`
- EN: `Fill in your friends and upload this file back. Tap /cancel to stop.`
- RU: `Заполните друзей и загрузите файл обратно. Нажмите /cancel для отмены.`
- ✅ Has `/cancel` hint. Parity OK.

### `bulk_cap_exceeded`
- EN: `⚠️ Only %d friend(s) added — friend limit of %d reached. Use /remove to free up a spot.`
- RU: `⚠️ Добавлено только %d друг(ов) — достигнут лимит в %d. Используйте /remove, чтобы освободить место.`
- ✅ Has next-step hint. Parity OK.

### `bulk_error_too_many` (RU)
- RU uses `\u26a0\ufe0f` + unicode escapes for the whole string. EN uses literal emoji + text.
- ⚠️ **Issue #7**: RU `bulk_error_too_many` is stored as full unicode escapes while all other RU messages use literal Cyrillic. Cosmetic inconsistency — works fine at runtime but makes the file harder to read/maintain.

**Session 7 result: ⚠️ 1 cosmetic issue (unicode escapes in RU `bulk_error_too_many`).**

---

## Session 8 — Export / Stats / Settings / Language

### `export_empty`
- EN: `👥 Nothing to export yet. Use /add to add your first friend, then come back here.`
- RU: `👥 Пока нечего экспортировать. Добавьте первого друга через /add и возвращайтесь.`
- ✅ Has next-step hint. Parity OK.

### `export_caption`
- EN: `Your friends list. You can edit and upload it back with /addmany.`
- RU: `Ваш список друзей. Вы можете отредактировать его и загрузить обратно через /addmany.`
- ✅ Parity OK.

### `export_failed` (RU)
- RU: `\u26a0\ufe0f Экспорт не удался. Пожалуйста, попробуйте позже.` — stored as unicode escapes.
- ⚠️ **Issue #8**: Same cosmetic issue as `bulk_error_too_many` — RU uses unicode escapes instead of literal text.

### `stats_header`
- EN: `📊 <b>Your Festiva stats</b>\n\n👥 Friends: <b>%d</b>\n🎂 Next birthday: %s\n📅 This month: <b>%d</b>\n🏆 Upcoming jubilees: <b>%d</b>`
- RU: `📊 <b>Ваша статистика</b>\n\n👥 Друзья: <b>%d</b>\n🎂 Следующий ДР: %s\n📅 В этом месяце: <b>%d</b>\n🏆 Юбилеи: <b>%d</b>`
- ✅ Double `\n\n` after header, single `\n` between lines. Parity OK.

### `settings_header`
- EN: `⏰ <b>Notification time</b>\n\nChoose when to receive daily reminders:`
- RU: `⏰ <b>Время уведомлений</b>\n\nВыберите время ежедневных напоминаний:`
- ✅ Double `\n\n`. Parity OK.

### `settings_hour_set` / `settings_tz_set`
- ✅ Has next-step hint (`/settings`). Parity OK.

### `settings_tz_header`
- EN: `🌍 <b>Timezone</b>\n\nChoose your timezone:`
- RU: `🌍 <b>Часовой пояс</b>\n\nВыберите часовой пояс:`
- ✅ Double `\n\n`. Parity OK.

### `language_choose`
- EN: `🌐 <b>Choose your language:</b>`
- RU: `🌐 <b>Выберите язык:</b>`
- ✅ Button-only state. No `/cancel` needed. Parity OK.

### `language_set`
- EN: `✅ Language set to <b>English</b> 🇬🇧. Use /language to change it again.`
- RU: `✅ Язык установлен: <b>Русский</b> 🇷🇺. Используйте /language для смены.`
- ✅ Has next-step hint. Parity OK.

**Session 8 result: ⚠️ 1 cosmetic issue (`export_failed` RU unicode escapes).**

---

## Session 9 — Notifications / System messages

### `notify_today` / `notify_tomorrow` / `notify_week`
- EN: `🎂 Today is <b>%s</b>%s's birthday %s — turning <b>%s</b>!\n👉 <a href="...">Open Festiva</a>`
- RU: `🎂 Сегодня день рождения у <b>%s</b>%s %s — исполняется <b>%s</b>!\n👉 <a href="...">Открыть Festiva</a>`
- ✅ Single `\n` before link. Parity OK.

### `cancel_active`
- EN: `<b><i>Operation cancelled. How else can I help? Send /help for commands.</i></b>`
- RU: `<b><i>Текущая команда отменена. Чем ещё могу помочь? Отправьте /help для списка команд.</i></b>`
- ✅ Parity OK.

### `cancel_idle`
- EN: `<b><i>Nothing to cancel right now. Use /add to get started.</i></b>`
- RU: `<b><i>Нечего отменять. Начните с /add.</i></b>`
- ✅ Parity OK.

### `unknown_command`
- EN: `<b>Unknown command.</b> Use /help for available commands.`
- RU: `<b>Неизвестная команда.</b> Используйте /help для списка доступных команд.`
- ✅ Parity OK.

### `session_expired`
- EN: `⏰ This action has expired. Please start again with the relevant command.`
- RU: `⏰ Это действие устарело. Пожалуйста, начните заново с нужной команды.`
- ✅ Parity OK.

### `use_buttons` (RU)
- RU stored as unicode escapes: `\ud83d\udc47 \u041f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430...`
- ⚠️ **Issue #9**: Same cosmetic issue — RU `use_buttons` uses unicode escapes instead of literal Cyrillic.

### `month_parse_error`
- ✅ Parity OK.

**Session 9 result: ⚠️ 1 cosmetic issue (`use_buttons` RU unicode escapes).**

---

## Session 10 — Delete Account

### `delete_account_ask`
- EN: `⚠️ <b>Delete your account?</b>\n\nThis will permanently remove all your friends and preferences. This cannot be undone.`
- RU: `⚠️ <b>Удалить аккаунт?</b>\n\nЭто навсегда удалит всех ваших друзей и настройки. Действие нельзя отменить.`
- ✅ Double `\n\n`. Parity OK.

### `delete_account_done`
- EN: `✅ Your account and all data have been deleted.`
- RU: `✅ Ваш аккаунт и все данные удалены.`
- ⚠️ **Issue #10**: No next-step hint. After deletion the user is left with nothing.
  - EN: add `Use /start to begin fresh.`
  - RU: add `Используйте /start, чтобы начать заново.`

### `delete_account_cancel`
- EN: `❌ Deletion cancelled. Your data is safe. Use /settings to adjust preferences or /help for commands.`
- RU: `❌ Удаление отменено. Ваши данные в безопасности. Используйте /settings для настройки или /help для списка команд.`
- ✅ Has next-step hints. Parity OK.

**Session 10 result: ⚠️ 1 issue on `delete_account_done`.**

---

## Full Issues Summary

| # | Key | Lang | Type | Description |
|---|---|---|---|---|
| 1 | `friend_added` | EN+RU | Missing hint | No next-step hint after adding a friend |
| 2 | `friend_added` | EN vs RU | Parity | EN: "added!" vs RU: "добавлен(а) в список!" (minor) |
| 3 | `friend_not_found` | EN+RU | Missing hint | No next-step hint |
| 4 | `friend_not_found` | EN | Punctuation | Uses straight `"` quotes; other messages use `\u201c\u201d` curly quotes |
| 5 | `edit_select` | EN+RU | Missing hint | No `/cancel` hint on the selection prompt |
| 6 | `birthdays_none` | EN+RU | Minor | Missing emoji at start (other none-messages have emoji) |
| 7 | `bulk_error_too_many` | RU | Cosmetic | Full unicode escapes instead of literal Cyrillic |
| 8 | `export_failed` | RU | Cosmetic | Full unicode escapes instead of literal Cyrillic |
| 9 | `use_buttons` | RU | Cosmetic | Full unicode escapes instead of literal Cyrillic |
| 10 | `delete_account_done` | EN+RU | Missing hint | No next-step hint after account deletion |

---

## Priority

**Fix now (functional UX):** #1, #3, #5, #10
**Fix now (correctness):** #4
**Fix later (cosmetic):** #2, #6, #7, #8, #9

---

## Progress

- [x] Session 1 — Welcome, Menu, About
- [x] Session 2 — Add Friend flow
- [x] Session 3 — Remove flow
- [x] Session 4 — Edit flow
- [x] Session 5 — List, Birthdays, Today, Upcoming, Jubilee
- [x] Session 6 — Search
- [x] Session 7 — Bulk Add
- [x] Session 8 — Export, Stats, Settings, Language
- [x] Session 9 — Notifications, System messages
- [x] Session 10 — Delete Account
