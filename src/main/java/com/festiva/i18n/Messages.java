package com.festiva.i18n;

import java.util.Map;

public final class Messages {

    private Messages() {}

    // Keys
    public static final String WELCOME               = "welcome";
    public static final String HELP                  = "help";
    public static final String ENTER_NAME            = "enter_name";
    public static final String ENTER_DATE            = "enter_date";
    public static final String NAME_EMPTY            = "name_empty";
    public static final String NAME_EXISTS           = "name_exists";
    public static final String DATE_FORMAT_ERROR     = "date_format_error";
    public static final String DATE_FUTURE_ERROR     = "date_future_error";
    public static final String FRIEND_ADDED          = "friend_added";
    public static final String FRIEND_NOT_FOUND      = "friend_not_found";
    public static final String FRIEND_REMOVED        = "friend_removed";
    public static final String FRIENDS_EMPTY         = "friends_empty";
    public static final String LIST_HEADER           = "list_header";
    public static final String LIST_TURNED           = "list_turned";
    public static final String LIST_WILL_TURN        = "list_will_turn";
    public static final String LIST_DAYS_TODAY       = "list_days_today";
    public static final String LIST_DAYS_LEFT        = "list_days_left";
    public static final String REMOVE_EMPTY_ADD     = "remove_empty_add";
    public static final String SELECT_REMOVE         = "select_remove";
    public static final String CONFIRM_REMOVE_ASK    = "confirm_remove_ask";
    public static final String CONFIRM_REMOVE_CANCEL = "confirm_remove_cancel";
    public static final String BIRTHDAYS_HEADER      = "birthdays_header";
    public static final String BIRTHDAYS_NONE        = "birthdays_none";
    public static final String CURRENT_MONTH         = "current_month";
    public static final String UPCOMING_TODAY        = "upcoming_today";
    public static final String UPCOMING_HEADER       = "upcoming_header";
    public static final String UPCOMING_NONE         = "upcoming_none";
    public static final String UPCOMING_TURNS        = "upcoming_turns";
    public static final String JUBILEE_HEADER        = "jubilee_header";
    public static final String JUBILEE_NONE          = "jubilee_none";
    public static final String JUBILEE_TURNS         = "jubilee_turns";
    public static final String CANCEL_ACTIVE         = "cancel_active";
    public static final String CANCEL_IDLE           = "cancel_idle";
    public static final String UNKNOWN_COMMAND       = "unknown_command";
    public static final String ADD_ERROR             = "add_error";
    public static final String LANGUAGE_CHOOSE       = "language_choose";
    public static final String LANGUAGE_SET          = "language_set";
    public static final String MONTH_PARSE_ERROR     = "month_parse_error";
    public static final String YEARS_OLD             = "years_old";
    public static final String BIRTHDAYS_PICK        = "birthdays_pick";
    public static final String NOTIFY_TODAY          = "notify_today";
    public static final String NOTIFY_TOMORROW       = "notify_tomorrow";
    public static final String NOTIFY_WEEK           = "notify_week";
    public static final String DATE_PICK_YEAR        = "date_pick_year";
    public static final String DATE_PICK_MONTH       = "date_pick_month";
    public static final String DATE_PICK_DAY         = "date_pick_day";
    public static final String DATE_PICK_BACK        = "date_pick_back";
    public static final String JUBILEE_DAYS_LEFT     = "jubilee_days_left";
    public static final String JUBILEE_DAYS_TODAY    = "jubilee_days_today";
    public static final String LIST_UPCOMING_HEADER  = "list_upcoming_header";
    public static final String LIST_CELEBRATED_HEADER = "list_celebrated_header";
    public static final String UPCOMING_DAYS_FILTER  = "upcoming_days_filter";
    public static final String EDIT_SELECT          = "edit_select";
    public static final String EDIT_CHOOSE_FIELD    = "edit_choose_field";
    public static final String EDIT_ENTER_NAME      = "edit_enter_name";
    public static final String EDIT_NAME_DONE       = "edit_name_done";
    public static final String EDIT_DATE_DONE       = "edit_date_done";
    public static final String YEARS_TURNS           = "years_turns";

    private static final Map<String, String> EN = Map.ofEntries(
        Map.entry(WELCOME,               "👋 <b>Welcome to Festiva!</b>\nI'll help you never forget your friends' birthdays.\n\n👥 <b>Friends:</b>\n/list — list friends\n/add — add a friend\n/remove — remove a friend\n/edit — edit a friend\n\n🎂 <b>Birthdays:</b>\n/birthdays — by month\n/upcomingbirthdays — upcoming\n/jubilee — milestones\n\n🌐 /language — change language\n/cancel — cancel current operation"),
        Map.entry(HELP,                  "📖 <b>Festiva commands:</b>\n\n👥 <b>Friends:</b>\n/list — list friends\n/add — add a friend\n/remove — remove a friend\n/edit — edit a friend\n\n🎂 <b>Birthdays:</b>\n/birthdays — by month\n/upcomingbirthdays — upcoming\n/jubilee — milestones\n\n🌐 /language — change language\n/cancel — cancel current operation"),
        Map.entry(ENTER_NAME,            "Enter your friend's name:"),
        Map.entry(ENTER_DATE,            "Enter %s's birth date in DD.MM.YYYY format\nExample: 15.03.1990"),
        Map.entry(NAME_EMPTY,            "Name cannot be empty. Enter a name or /cancel."),
        Map.entry(NAME_EXISTS,           "A friend named \"%s\" already exists. Enter a different name or /cancel."),
        Map.entry(DATE_FORMAT_ERROR,     "Invalid date format. Use DD.MM.YYYY, e.g. 15.03.1990"),
        Map.entry(DATE_FUTURE_ERROR,     "Birth date cannot be in the future."),
        Map.entry(FRIEND_ADDED,          "✅ %s added!"),
        Map.entry(FRIEND_NOT_FOUND,      "Friend \"%s\" not found."),
        Map.entry(FRIEND_REMOVED,        "✅ <b>%s</b> removed!"),
        Map.entry(FRIENDS_EMPTY,         "<b>Friend list is empty.</b>"),
        Map.entry(LIST_HEADER,           "<b>Friends (current calendar year):</b>\n\n"),
        Map.entry(LIST_TURNED,           "(turned <b>%d</b> this year)"),
        Map.entry(LIST_WILL_TURN,        "(currently <b>%d</b>, turns <b>%d</b> this year)"),
        Map.entry(LIST_DAYS_TODAY,       "🎂"),
        Map.entry(LIST_DAYS_LEFT,        "(in %dd)"),
        Map.entry(SELECT_REMOVE,         "Select a friend to remove:"),
        Map.entry(CONFIRM_REMOVE_ASK,    "Remove <b>%s</b>? This cannot be undone."),
        Map.entry(CONFIRM_REMOVE_CANCEL, "Removal cancelled."),
        Map.entry(BIRTHDAYS_HEADER,      "🎂 <b>Birthdays — %s</b>\n\n"),
        Map.entry(BIRTHDAYS_NONE,        "No birthdays in <b>%s</b>."),
        Map.entry(CURRENT_MONTH,         "Current month"),
        Map.entry(UPCOMING_HEADER,       "<b>Upcoming birthdays:</b>\n\n"),
        Map.entry(UPCOMING_NONE,         "<b>No birthdays in the next %d days.</b>"),
        Map.entry(UPCOMING_TURNS,        "(turns <b>%d</b>, days left — <b>%d</b>)"),
        Map.entry(UPCOMING_TODAY,         "🎂 <b>TODAY!</b> turns <b>%d</b>"),
        Map.entry(REMOVE_EMPTY_ADD,       "➕ Add a friend"),
        Map.entry(JUBILEE_HEADER,        "<b>Milestone birthdays</b>\n\n"),
        Map.entry(JUBILEE_NONE,          "<b>No upcoming milestone birthdays.</b>"),
        Map.entry(JUBILEE_TURNS,         "(turns <b>%d</b>)"),
        Map.entry(CANCEL_ACTIVE,         "<b><i>Operation cancelled. How else can I help? Send /help for commands.</i></b>"),
        Map.entry(CANCEL_IDLE,           "<b><i>Nothing to cancel. I wasn't doing anything. Zzzzz...</i></b>"),
        Map.entry(UNKNOWN_COMMAND,       "<b>Unknown command.</b> Use /help for available commands."),
        Map.entry(ADD_ERROR,             "Something went wrong. Start over with /add."),
        Map.entry(LANGUAGE_CHOOSE,       "🌐 <b>Choose your language:</b>"),
        Map.entry(LANGUAGE_SET,          "✅ Language set to <b>English</b> 🇬🇧"),
        Map.entry(MONTH_PARSE_ERROR,     "Error selecting month."),
        Map.entry(YEARS_OLD,             "%d years old"),
        Map.entry(BIRTHDAYS_PICK,        "<b>View birthdays</b>\n\nSelect a month:"),
        Map.entry(NOTIFY_TODAY,          "🎂 Today is <b>%s</b>'s birthday %s — turning <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>"),
        Map.entry(NOTIFY_TOMORROW,       "🔔 Tomorrow is <b>%s</b>'s birthday %s — turning <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>"),
        Map.entry(NOTIFY_WEEK,           "📅 In one week it's <b>%s</b>'s birthday %s — turning <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Open Festiva</a>"),
        Map.entry(DATE_PICK_YEAR,        "Select <b>%s</b>'s birth year:"),
        Map.entry(DATE_PICK_MONTH,       "Select <b>%s</b>'s birth month:"),
        Map.entry(DATE_PICK_DAY,         "Select <b>%s</b>'s birth day:"),
        Map.entry(DATE_PICK_BACK,        "← Back"),
        Map.entry(JUBILEE_DAYS_LEFT,     "(in <b>%d</b>d)"),
        Map.entry(JUBILEE_DAYS_TODAY,    "🎂"),
        Map.entry(LIST_UPCOMING_HEADER,  "<b>Coming up:</b>\n"),
        Map.entry(LIST_CELEBRATED_HEADER, "\n<b>Already celebrated:</b>\n"),
        Map.entry(YEARS_TURNS,           "turns <b>%d</b>"),
        Map.entry(EDIT_SELECT,            "Select a friend to edit:"),
        Map.entry(EDIT_CHOOSE_FIELD,      "Edit <b>%s</b> (%s) — what would you like to change?"),
        Map.entry(EDIT_ENTER_NAME,        "Enter a new name for <b>%s</b>:"),
        Map.entry(EDIT_NAME_DONE,         "✅ Name updated to <b>%s</b>!"),
        Map.entry(EDIT_DATE_DONE,         "✅ Birth date for <b>%s</b> updated!"),
        Map.entry(UPCOMING_DAYS_FILTER,   "Show birthdays in the next:")
    );

    private static final Map<String, String> RU = Map.ofEntries(
        Map.entry(WELCOME,               "👋 <b>Добро пожаловать в Festiva!</b>\nЯ помогу вам не забыть дни рождения друзей.\n\n👥 <b>Друзья:</b>\n/list — список друзей\n/add — добавить друга\n/remove — удалить друга\n/edit — редактировать друга\n\n🎂 <b>Дни рождения:</b>\n/birthdays — по месяцам\n/upcomingbirthdays — ближайшие\n/jubilee — юбилейные\n\n🌐 /language — сменить язык\n/cancel — отменить текущую операцию"),
        Map.entry(HELP,                  "📖 <b>Команды Festiva:</b>\n\n👥 <b>Друзья:</b>\n/list — список друзей\n/add — добавить друга\n/remove — удалить друга\n/edit — редактировать друга\n\n🎂 <b>Дни рождения:</b>\n/birthdays — по месяцам\n/upcomingbirthdays — ближайшие\n/jubilee — юбилейные\n\n🌐 /language — сменить язык\n/cancel — отменить текущую операцию"),
        Map.entry(ENTER_NAME,            "Введите имя друга:"),
        Map.entry(ENTER_DATE,            "Введите дату рождения %s в формате ДД.ММ.ГГГГ\nНапример: 15.03.1990"),
        Map.entry(NAME_EMPTY,            "Имя не может быть пустым. Введите имя или /cancel для отмены."),
        Map.entry(NAME_EXISTS,           "Друг с именем \"%s\" уже существует. Введите другое имя или /cancel."),
        Map.entry(DATE_FORMAT_ERROR,     "Неверный формат даты. Используйте ДД.ММ.ГГГГ, например: 15.03.1990"),
        Map.entry(DATE_FUTURE_ERROR,     "Дата рождения не может быть в будущем."),
        Map.entry(FRIEND_ADDED,          "✅ %s добавлен!"),
        Map.entry(FRIEND_NOT_FOUND,      "Друг \"%s\" не найден."),
        Map.entry(FRIEND_REMOVED,        "✅ <b>%s</b> удалён!"),
        Map.entry(FRIENDS_EMPTY,         "<b>Список друзей пуст.</b>"),
        Map.entry(LIST_HEADER,           "<b>Список друзей (текущий календарный год):</b>\n\n"),
        Map.entry(LIST_TURNED,           "(в этом году исполнилось <b>%d</b>)"),
        Map.entry(LIST_WILL_TURN,        "(сейчас <b>%d</b>, в этом году исполнится <b>%d</b>)"),
        Map.entry(LIST_DAYS_TODAY,       "🎂"),
        Map.entry(LIST_DAYS_LEFT,        "(через %d д)"),
        Map.entry(SELECT_REMOVE,         "Выберите друга для удаления:"),
        Map.entry(CONFIRM_REMOVE_ASK,    "Удалить <b>%s</b>? Это действие нельзя отменить."),
        Map.entry(CONFIRM_REMOVE_CANCEL, "Удаление отменено."),
        Map.entry(BIRTHDAYS_HEADER,      "🎂 <b>Дни рождения — %s</b>\n\n"),
        Map.entry(BIRTHDAYS_NONE,        "В <b>%s</b> нет дней рождения."),
        Map.entry(CURRENT_MONTH,         "Текущий месяц"),
        Map.entry(UPCOMING_HEADER,       "<b>Ближайшие дни рождения:</b>\n\n"),
        Map.entry(UPCOMING_NONE,         "<b>В ближайшие %d дней нет дней рождения.</b>"),
        Map.entry(UPCOMING_TURNS,        "(исполнится <b>%d</b>, дней до дня рождения — <b>%d</b>)"),
        Map.entry(UPCOMING_TODAY,         "🎂 <b>СЕГОДНЯ!</b> исполняется <b>%d</b>"),
        Map.entry(REMOVE_EMPTY_ADD,       "➕ Добавить друга"),
        Map.entry(JUBILEE_HEADER,        "<b>Юбилейные дни рождения</b>\n\n"),
        Map.entry(JUBILEE_NONE,          "<b>В ближайшее время нет юбилейных дней рождения.</b>"),
        Map.entry(JUBILEE_TURNS,         "(исполнится <b>%d</b> лет)"),
        Map.entry(CANCEL_ACTIVE,         "<b><i>Текущая команда отменена. Чем ещё могу помочь? Отправьте /help для списка команд.</i></b>"),
        Map.entry(CANCEL_IDLE,           "<b><i>Нет активной команды для отмены. Я и так ничего не делал. Zzzzz...</i></b>"),
        Map.entry(UNKNOWN_COMMAND,       "<b>Неизвестная команда.</b> Используйте /help для списка доступных команд."),
        Map.entry(ADD_ERROR,             "Что-то пошло не так. Начните заново с /add."),
        Map.entry(LANGUAGE_CHOOSE,       "🌐 <b>Выберите язык:</b>"),
        Map.entry(LANGUAGE_SET,          "✅ Язык установлен: <b>Русский</b> 🇷🇺"),
        Map.entry(MONTH_PARSE_ERROR,     "Ошибка при выборе месяца."),
        Map.entry(YEARS_OLD,             "%d лет"),
        Map.entry(BIRTHDAYS_PICK,        "<b>Просмотр дней рождения</b>\n\nВыберите месяц:"),
        Map.entry(NOTIFY_TODAY,          "🎂 Сегодня день рождения у <b>%s</b> %s — исполняется <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>"),
        Map.entry(NOTIFY_TOMORROW,       "🔔 Завтра день рождения у <b>%s</b> %s — исполняется <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>"),
        Map.entry(NOTIFY_WEEK,           "📅 Через неделю день рождения у <b>%s</b> %s — исполняется <b>%d</b>!\n👉 <a href=\"https://t.me/%s\">Открыть Festiva</a>"),
        Map.entry(DATE_PICK_YEAR,        "Выберите год рождения <b>%s</b>:"),
        Map.entry(DATE_PICK_MONTH,       "Выберите месяц рождения <b>%s</b>:"),
        Map.entry(DATE_PICK_DAY,         "Выберите день рождения <b>%s</b>:"),
        Map.entry(DATE_PICK_BACK,        "← Назад"),
        Map.entry(JUBILEE_DAYS_LEFT,     "(через <b>%d</b> д)"),
        Map.entry(JUBILEE_DAYS_TODAY,    "🎂"),
        Map.entry(LIST_UPCOMING_HEADER,  "<b>Предстоящие:</b>\n"),
        Map.entry(LIST_CELEBRATED_HEADER, "\n<b>Уже отметили:</b>\n"),
        Map.entry(YEARS_TURNS,           "исполнится <b>%d</b>"),
        Map.entry(EDIT_SELECT,            "Выберите друга для редактирования:"),
        Map.entry(EDIT_CHOOSE_FIELD,      "Редактировать <b>%s</b> (%s) — что изменить?"),
        Map.entry(EDIT_ENTER_NAME,        "Введите новое имя для <b>%s</b>:"),
        Map.entry(EDIT_NAME_DONE,         "✅ Имя обновлено на <b>%s</b>!"),
        Map.entry(EDIT_DATE_DONE,         "✅ Дата рождения <b>%s</b> обновлена!"),
        Map.entry(UPCOMING_DAYS_FILTER,   "Показать дни рождения в ближайшие:")
    );

    public static String get(Lang lang, String key) {
        return (lang == Lang.EN ? EN : RU).getOrDefault(key, key);
    }

    public static String get(Lang lang, String key, Object... args) {
        return String.format(get(lang, key), args);
    }
}
