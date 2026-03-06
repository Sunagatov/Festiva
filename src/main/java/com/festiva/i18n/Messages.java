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
    public static final String SELECT_REMOVE         = "select_remove";
    public static final String BIRTHDAYS_HEADER      = "birthdays_header";
    public static final String BIRTHDAYS_NONE        = "birthdays_none";
    public static final String CURRENT_MONTH         = "current_month";
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

    private static final Map<String, String> EN = Map.ofEntries(
        Map.entry(WELCOME,           "👋 <b>Welcome to Festiva!</b>\nI'll help you never forget your friends' birthdays.\n\n👥 <b>Friends:</b>\n/list — list friends\n/add — add a friend\n/remove — remove a friend\n\n🎂 <b>Birthdays:</b>\n/birthdays — by month\n/upcomingbirthdays — upcoming\n/jubilee — milestones\n\n🌐 /language — change language\n/cancel — cancel current operation"),
        Map.entry(HELP,              "📖 <b>Festiva commands:</b>\n\n👥 <b>Friends:</b>\n/list — list friends\n/add — add a friend\n/remove — remove a friend\n\n🎂 <b>Birthdays:</b>\n/birthdays — by month\n/upcomingbirthdays — upcoming\n/jubilee — milestones\n\n🌐 /language — change language\n/cancel — cancel current operation"),
        Map.entry(ENTER_NAME,        "Enter your friend's name:"),
        Map.entry(ENTER_DATE,        "Enter %s's birth date in DD.MM.YYYY format\nExample: 15.03.1990"),
        Map.entry(NAME_EMPTY,        "Name cannot be empty. Enter a name or /cancel."),
        Map.entry(NAME_EXISTS,       "A friend named \"%s\" already exists. Enter a different name or /cancel."),
        Map.entry(DATE_FORMAT_ERROR, "Invalid date format. Use DD.MM.YYYY, e.g. 15.03.1990"),
        Map.entry(DATE_FUTURE_ERROR, "Birth date cannot be in the future."),
        Map.entry(FRIEND_ADDED,      "✅ %s added!"),
        Map.entry(FRIEND_NOT_FOUND,  "Friend \"%s\" not found."),
        Map.entry(FRIEND_REMOVED,    "✅ <b>%s</b> removed!"),
        Map.entry(FRIENDS_EMPTY,     "<b>Friend list is empty.</b>"),
        Map.entry(LIST_HEADER,       "<b>Friends (current calendar year):</b>\n\n"),
        Map.entry(LIST_TURNED,       "(turned <b>%d</b> this year)"),
        Map.entry(LIST_WILL_TURN,    "(currently <b>%d</b>, turns <b>%d</b> this year)"),
        Map.entry(SELECT_REMOVE,     "Select a friend to remove:"),
        Map.entry(BIRTHDAYS_HEADER,  "🎂 <b>Birthdays — %s</b>\n\n"),
        Map.entry(BIRTHDAYS_NONE,    "No birthdays in <b>%s</b>."),
        Map.entry(CURRENT_MONTH,     "Current month"),
        Map.entry(UPCOMING_HEADER,   "<b>Upcoming birthdays:</b>\n\n"),
        Map.entry(UPCOMING_NONE,     "<b>No birthdays in the next %d days.</b>"),
        Map.entry(UPCOMING_TURNS,    "(turns <b>%d</b>, days left — <b>%d</b>)"),
        Map.entry(JUBILEE_HEADER,    "<b>Milestone birthdays</b>\n\n"),
        Map.entry(JUBILEE_NONE,      "<b>No upcoming milestone birthdays.</b>"),
        Map.entry(JUBILEE_TURNS,     "(turns <b>%d</b>)"),
        Map.entry(CANCEL_ACTIVE,     "<b><i>Operation cancelled. How else can I help? Send /help for commands.</i></b>"),
        Map.entry(CANCEL_IDLE,       "<b><i>Nothing to cancel. I wasn't doing anything. Zzzzz...</i></b>"),
        Map.entry(UNKNOWN_COMMAND,   "<b>Unknown command.</b> Use /help for available commands."),
        Map.entry(ADD_ERROR,         "Something went wrong. Start over with /add."),
        Map.entry(LANGUAGE_CHOOSE,   "🌐 <b>Choose your language:</b>"),
        Map.entry(LANGUAGE_SET,      "✅ Language set to <b>English</b> 🇬🇧"),
        Map.entry(MONTH_PARSE_ERROR, "Error selecting month."),
        Map.entry(YEARS_OLD,         "%d years old"),
        Map.entry(BIRTHDAYS_PICK,    "<b>View birthdays</b>\n\nSelect a month:"),
        Map.entry(NOTIFY_TODAY,       "🎂 Today is your friend %s's birthday!"),
        Map.entry(NOTIFY_TOMORROW,    "🔔 Tomorrow is your friend %s's birthday!"),
        Map.entry(NOTIFY_WEEK,        "📅 In one week it's your friend %s's birthday!")
    );

    private static final Map<String, String> RU = Map.ofEntries(
        Map.entry(WELCOME,           "👋 <b>Добро пожаловать в Festiva!</b>\nЯ помогу вам не забыть дни рождения друзей.\n\n👥 <b>Друзья:</b>\n/list — список друзей\n/add — добавить друга\n/remove — удалить друга\n\n🎂 <b>Дни рождения:</b>\n/birthdays — по месяцам\n/upcomingbirthdays — ближайшие\n/jubilee — юбилейные\n\n🌐 /language — сменить язык\n/cancel — отменить текущую операцию"),
        Map.entry(HELP,              "📖 <b>Команды Festiva:</b>\n\n👥 <b>Друзья:</b>\n/list — список друзей\n/add — добавить друга\n/remove — удалить друга\n\n🎂 <b>Дни рождения:</b>\n/birthdays — по месяцам\n/upcomingbirthdays — ближайшие\n/jubilee — юбилейные\n\n🌐 /language — сменить язык\n/cancel — отменить текущую операцию"),
        Map.entry(ENTER_NAME,        "Введите имя друга:"),
        Map.entry(ENTER_DATE,        "Введите дату рождения %s в формате ДД.ММ.ГГГГ\nНапример: 15.03.1990"),
        Map.entry(NAME_EMPTY,        "Имя не может быть пустым. Введите имя или /cancel для отмены."),
        Map.entry(NAME_EXISTS,       "Друг с именем \"%s\" уже существует. Введите другое имя или /cancel."),
        Map.entry(DATE_FORMAT_ERROR, "Неверный формат даты. Используйте ДД.ММ.ГГГГ, например: 15.03.1990"),
        Map.entry(DATE_FUTURE_ERROR, "Дата рождения не может быть в будущем."),
        Map.entry(FRIEND_ADDED,      "✅ %s добавлен!"),
        Map.entry(FRIEND_NOT_FOUND,  "Друг \"%s\" не найден."),
        Map.entry(FRIEND_REMOVED,    "✅ <b>%s</b> удалён!"),
        Map.entry(FRIENDS_EMPTY,     "<b>Список друзей пуст.</b>"),
        Map.entry(LIST_HEADER,       "<b>Список друзей (текущий календарный год):</b>\n\n"),
        Map.entry(LIST_TURNED,       "(в этом году исполнилось <b>%d</b>)"),
        Map.entry(LIST_WILL_TURN,    "(сейчас <b>%d</b>, в этом году исполнится <b>%d</b>)"),
        Map.entry(SELECT_REMOVE,     "Выберите друга для удаления:"),
        Map.entry(BIRTHDAYS_HEADER,  "🎂 <b>Дни рождения — %s</b>\n\n"),
        Map.entry(BIRTHDAYS_NONE,    "В <b>%s</b> нет дней рождения."),
        Map.entry(CURRENT_MONTH,     "Текущий месяц"),
        Map.entry(UPCOMING_HEADER,   "<b>Ближайшие дни рождения:</b>\n\n"),
        Map.entry(UPCOMING_NONE,     "<b>В ближайшие %d дней нет дней рождения.</b>"),
        Map.entry(UPCOMING_TURNS,    "(исполнится <b>%d</b>, дней до дня рождения — <b>%d</b>)"),
        Map.entry(JUBILEE_HEADER,    "<b>Юбилейные дни рождения</b>\n\n"),
        Map.entry(JUBILEE_NONE,      "<b>В ближайшее время нет юбилейных дней рождения.</b>"),
        Map.entry(JUBILEE_TURNS,     "(исполнится <b>%d</b> лет)"),
        Map.entry(CANCEL_ACTIVE,     "<b><i>Текущая команда отменена. Чем ещё могу помочь? Отправьте /help для списка команд.</i></b>"),
        Map.entry(CANCEL_IDLE,       "<b><i>Нет активной команды для отмены. Я и так ничего не делал. Zzzzz...</i></b>"),
        Map.entry(UNKNOWN_COMMAND,   "<b>Неизвестная команда.</b> Используйте /help для списка доступных команд."),
        Map.entry(ADD_ERROR,         "Что-то пошло не так. Начните заново с /add."),
        Map.entry(LANGUAGE_CHOOSE,   "🌐 <b>Выберите язык:</b>"),
        Map.entry(LANGUAGE_SET,      "✅ Язык установлен: <b>Русский</b> 🇷🇺"),
        Map.entry(MONTH_PARSE_ERROR, "Ошибка при выборе месяца."),
        Map.entry(YEARS_OLD,         "%d лет"),
        Map.entry(BIRTHDAYS_PICK,    "<b>Просмотр дней рождения</b>\n\nВыберите месяц:"),
        Map.entry(NOTIFY_TODAY,       "🎂 Сегодня день рождения у вашего друга %s!"),
        Map.entry(NOTIFY_TOMORROW,    "🔔 Завтра день рождения у вашего друга %s!"),
        Map.entry(NOTIFY_WEEK,        "📅 Через неделю день рождения у вашего друга %s!")
    );

    public static String get(Lang lang, String key) {
        return (lang == Lang.EN ? EN : RU).getOrDefault(key, key);
    }

    public static String get(Lang lang, String key, Object... args) {
        return String.format(get(lang, key), args);
    }
}
