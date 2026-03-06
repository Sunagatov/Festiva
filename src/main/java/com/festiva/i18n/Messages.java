package com.festiva.i18n;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public final class Messages {

    public static final String RELATIONSHIP_PICK     = "relationship_pick";
    public static final String RELATIONSHIP_SKIP     = "relationship_skip";
    public static final String CONFIRM_YES           = "confirm_yes";
    public static final String CONFIRM_NO            = "confirm_no";
    public static final String EDIT_FIELD_NAME_BTN   = "edit_field_name_btn";
    public static final String EDIT_FIELD_DATE_BTN   = "edit_field_date_btn";
    public static final String EDIT_NOTIFS_ON        = "edit_notifs_on";
    public static final String EDIT_NOTIFS_OFF       = "edit_notifs_off";
    public static final String NOTIFY_STATUS_ON      = "notify_status_on";
    public static final String NOTIFY_STATUS_OFF     = "notify_status_off";
    public static final String QUICK_ADD_ANOTHER     = "quick_add_another";
    public static final String QUICK_LIST            = "quick_list";
    public static final String SETTINGS_TZ_HEADER    = "settings_tz_header";
    public static final String SETTINGS_TZ_SET       = "settings_tz_set";
    public static final String LANG_EN_BTN           = "lang_en_btn";
    public static final String LANG_RU_BTN           = "lang_ru_btn";
    public static final String EDIT_NOTIFY_TOGGLED   = "edit_notify_toggled";
    public static final String FRIEND_CAP            = "friend_cap";
    public static final String LIST_SORT_DATE        = "list_sort_date";
    public static final String LIST_SORT_NAME        = "list_sort_name";
    public static final String SEARCH_PROMPT         = "search_prompt";
    public static final String SEARCH_RESULTS        = "search_results";
    public static final String SEARCH_NONE           = "search_none";
    public static final String STATS_HEADER          = "stats_header";
    public static final String SETTINGS_HEADER       = "settings_header";
    public static final String SETTINGS_HOUR_SET     = "settings_hour_set";
    public static final String START_ADD_FIRST       = "start_add_first";
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
    public static final String REMOVE_EMPTY_ADD      = "remove_empty_add";
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
    public static final String DATE_YEAR_EARLIER     = "date_year_earlier";
    public static final String DATE_YEAR_LATER       = "date_year_later";
    public static final String JUBILEE_DAYS_LEFT     = "jubilee_days_left";
    public static final String JUBILEE_DAYS_TODAY    = "jubilee_days_today";
    public static final String LIST_UPCOMING_HEADER  = "list_upcoming_header";
    public static final String LIST_CELEBRATED_HEADER = "list_celebrated_header";
    public static final String UPCOMING_DAYS_FILTER  = "upcoming_days_filter";
    public static final String UPCOMING_DAYS_SUFFIX  = "upcoming_days_suffix";
    public static final String EDIT_SELECT           = "edit_select";
    public static final String EDIT_CHOOSE_FIELD     = "edit_choose_field";
    public static final String EDIT_ENTER_NAME       = "edit_enter_name";
    public static final String EDIT_NAME_DONE        = "edit_name_done";
    public static final String EDIT_DATE_DONE        = "edit_date_done";
    public static final String YEARS_TURNS           = "years_turns";

    private static MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        Messages.messageSource = messageSource;
    }

    /** For use in tests only — initializes without Spring context. */
    static void initForTest(MessageSource ms) {
        messageSource = ms;
    }

    public static String get(Lang lang, String key) {
        return messageSource.getMessage(key, null, key, lang.locale()).replace("\\n", "\n");
    }

    public static String get(Lang lang, String key, Object... args) {
        String pattern = messageSource.getMessage(key, null, key, lang.locale()).replace("\\n", "\n");
        return String.format(pattern, args);
    }
}
