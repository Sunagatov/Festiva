package com.festiva.i18n;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class Messages {

    public static final String RELATIONSHIP_PICK      = "relationship_pick";
    public static final String RELATIONSHIP_SKIP      = "relationship_skip";
    public static final String CONFIRM_YES            = "confirm_yes";
    public static final String CONFIRM_NO             = "confirm_no";
    public static final String EDIT_FIELD_NAME_BTN    = "edit_field_name_btn";
    public static final String EDIT_FIELD_DATE_BTN    = "edit_field_date_btn";
    public static final String EDIT_NOTIFS_ON         = "edit_notifs_on";
    public static final String EDIT_NOTIFS_OFF        = "edit_notifs_off";
    public static final String NOTIFY_STATUS_ON       = "notify_status_on";
    public static final String NOTIFY_STATUS_OFF      = "notify_status_off";
    public static final String QUICK_ADD_ANOTHER      = "quick_add_another";
    public static final String QUICK_LIST             = "quick_list";
    public static final String SETTINGS_TZ_HEADER     = "settings_tz_header";
    public static final String SETTINGS_TZ_SET        = "settings_tz_set";
    public static final String LANG_EN_BTN            = "lang_en_btn";
    public static final String LANG_RU_BTN            = "lang_ru_btn";
    public static final String EDIT_NOTIFY_TOGGLED    = "edit_notify_toggled";
    public static final String FRIEND_CAP             = "friend_cap";
    public static final String LIST_SORT_DATE         = "list_sort_date";
    public static final String LIST_SORT_NAME         = "list_sort_name";
    public static final String SEARCH_PROMPT          = "search_prompt";
    public static final String SEARCH_RESULTS         = "search_results";
    public static final String SEARCH_RESULTS_HINT    = "search_results_hint";
    public static final String SEARCH_NONE            = "search_none";
    public static final String SEARCH_TOO_LONG        = "search_too_long";
    public static final String STATS_HEADER           = "stats_header";
    public static final String SETTINGS_HEADER        = "settings_header";
    public static final String SETTINGS_HOUR_SET      = "settings_hour_set";
    public static final String WELCOME                = "welcome";
    public static final String MENU                   = "menu";
    public static final String ABOUT                  = "about";
    public static final String ENTER_NAME             = "enter_name";
    public static final String NAME_EMPTY             = "name_empty";
    public static final String NAME_TOO_LONG          = "name_too_long";
    public static final String NAME_EXISTS            = "name_exists";
    public static final String DATE_FUTURE_ERROR      = "date_future_error";
    public static final String FRIEND_ADDED           = "friend_added";
    public static final String FRIEND_NOT_FOUND       = "friend_not_found";
    public static final String FRIEND_REMOVED         = "friend_removed";
    public static final String FRIENDS_EMPTY          = "friends_empty";
    public static final String LIST_HEADER            = "list_header";
    public static final String LIST_TURNED            = "list_turned";
    public static final String LIST_WILL_TURN         = "list_will_turn";
    public static final String LIST_DAYS_TODAY        = "list_days_today";
    public static final String LIST_DAYS_LEFT         = "list_days_left";
    public static final String REMOVE_EMPTY_ADD       = "remove_empty_add";
    public static final String SELECT_REMOVE          = "select_remove";
    public static final String CONFIRM_REMOVE_ASK     = "confirm_remove_ask";
    public static final String CONFIRM_REMOVE_CANCEL  = "confirm_remove_cancel";
    public static final String BIRTHDAYS_HEADER       = "birthdays_header";
    public static final String BIRTHDAYS_NONE         = "birthdays_none";
    public static final String CURRENT_MONTH          = "current_month";
    public static final String UPCOMING_TODAY         = "upcoming_today";
    public static final String UPCOMING_HEADER        = "upcoming_header";
    public static final String UPCOMING_NONE          = "upcoming_none";
    public static final String UPCOMING_TURNS         = "upcoming_turns";
    public static final String JUBILEE_HEADER         = "jubilee_header";
    public static final String JUBILEE_NONE           = "jubilee_none";
    public static final String JUBILEE_TURNS          = "jubilee_turns";
    public static final String CANCEL_ACTIVE          = "cancel_active";
    public static final String CANCEL_IDLE            = "cancel_idle";
    public static final String UNKNOWN_COMMAND        = "unknown_command";
    public static final String SESSION_EXPIRED         = "session_expired";
    public static final String LANGUAGE_CHOOSE        = "language_choose";
    public static final String LANGUAGE_SET           = "language_set";
    public static final String MONTH_PARSE_ERROR      = "month_parse_error";
    public static final String YEARS_OLD              = "years_old";
    public static final String BIRTHDAYS_PICK         = "birthdays_pick";
    public static final String NOTIFY_TODAY           = "notify_today";
    public static final String NOTIFY_TOMORROW        = "notify_tomorrow";
    public static final String NOTIFY_WEEK            = "notify_week";
    public static final String DATE_PICK_YEAR         = "date_pick_year";
    public static final String DATE_PICK_MONTH        = "date_pick_month";
    public static final String DATE_PICK_DAY          = "date_pick_day";
    public static final String DATE_PICK_BACK         = "date_pick_back";
    public static final String DATE_YEAR_EARLIER      = "date_year_earlier";
    public static final String DATE_YEAR_LATER        = "date_year_later";
    public static final String JUBILEE_DAYS_LEFT      = "jubilee_days_left";
    public static final String JUBILEE_DAYS_TODAY     = "jubilee_days_today";
    public static final String LIST_UPCOMING_HEADER   = "list_upcoming_header";
    public static final String LIST_CELEBRATED_HEADER = "list_celebrated_header";
    public static final String UPCOMING_DAYS_SUFFIX   = "upcoming_days_suffix";
    public static final String EDIT_SELECT            = "edit_select";
    public static final String EDIT_CHOOSE_FIELD      = "edit_choose_field";
    public static final String EDIT_ENTER_NAME        = "edit_enter_name";
    public static final String EDIT_NAME_DONE         = "edit_name_done";
    public static final String EDIT_DATE_DONE         = "edit_date_done";
    public static final String YEARS_TURNS            = "years_turns";
    public static final String BULK_ADD_PROMPT        = "bulk_add_prompt";
    public static final String BULK_ADD_SUCCESS       = "bulk_add_success";
    public static final String BULK_ADD_ERRORS        = "bulk_add_errors";
    public static final String BULK_ADD_EMPTY         = "bulk_add_empty";
    public static final String BULK_ADD_FILE_INVALID  = "bulk_add_file_invalid";
    public static final String BULK_ADD_CHOOSE        = "bulk_add_choose";
    public static final String BULK_ADD_PASTE_BTN     = "bulk_add_paste_btn";
    public static final String BULK_ADD_CSV_BTN       = "bulk_add_csv_btn";
    public static final String BULK_ADD_ICS_BTN       = "bulk_add_ics_btn";
    public static final String BULK_ADD_CSV_CAPTION   = "bulk_add_csv_caption";
    public static final String EXPORT_CAPTION         = "export_caption";
    public static final String EXPORT_EMPTY           = "export_empty";
    public static final String EXPORT_FAILED          = "export_failed";
    public static final String EDIT_FIELD_REL_BTN     = "edit_field_rel_btn";
    public static final String EDIT_REL_DONE          = "edit_rel_done";
    public static final String LIST_PAGE              = "list_page";
    public static final String BULK_CAP_EXCEEDED      = "bulk_cap_exceeded";
    public static final String DELETE_ACCOUNT_ASK    = "delete_account_ask";
    public static final String DELETE_ACCOUNT_DONE   = "delete_account_done";
    public static final String DELETE_ACCOUNT_CANCEL = "delete_account_cancel";
    public static final String TODAY_HEADER            = "today_header";
    public static final String TODAY_NONE              = "today_none";
    public static final String TODAY_HINT              = "today_hint";
    public static final String BULK_ERROR_NO_DATA      = "bulk_error_no_data";
    public static final String BULK_ERROR_TOO_MANY     = "bulk_error_too_many";
    public static final String BULK_ERROR_FORMAT       = "bulk_error_format";
    public static final String BULK_ERROR_NAME_EMPTY   = "bulk_error_name_empty";
    public static final String BULK_ERROR_NAME_LONG    = "bulk_error_name_long";
    public static final String BULK_ERROR_DATE_INVALID = "bulk_error_date_invalid";
    public static final String BULK_ERROR_DATE_FUTURE  = "bulk_error_date_future";
    public static final String BULK_ERROR_EXISTS       = "bulk_error_exists";
    public static final String BULK_ERROR_DUPLICATE    = "bulk_error_duplicate";
    public static final String BULK_ERROR_RELATIONSHIP_INVALID = "bulk_error_relationship_invalid";
    public static final String USE_BUTTONS                    = "use_buttons";
    public static final String ICS_PROMPT          = "ics_prompt";
    public static final String ICS_NOT_A_FILE      = "ics_not_a_file";
    public static final String ICS_WRONG_TYPE      = "ics_wrong_type";
    public static final String ICS_TOO_LARGE       = "ics_too_large";
    public static final String ICS_PARSE_ERROR     = "ics_parse_error";
    public static final String ICS_NO_EVENTS       = "ics_no_events";
    public static final String ICS_PREVIEW         = "ics_preview";
    public static final String ICS_PREVIEW_NO_VALID = "ics_preview_no_valid";
    public static final String ICS_CONFIRM_BTN     = "ics_confirm_btn";
    public static final String ICS_DONE            = "ics_done";
    public static final String ICS_CANCELLED       = "ics_cancelled";
    public static final String YEARS_OLD_ONE           = "years_old_one";
    public static final String YEARS_OLD_FEW           = "years_old_few";
    public static final String YEARS_OLD_MANY          = "years_old_many";

    private static MessageSource messageSource;

    @Autowired
    Messages(MessageSource messageSource) {
        Messages.messageSource = messageSource;
    }

    static void initForTest(MessageSource ms) {
        messageSource = ms;
    }

    public static String get(Lang lang, String key) {
        return Objects.requireNonNullElse(messageSource.getMessage(key, null, key, lang.locale()), key).replace("\\n", "\n");
    }

    public static String get(Lang lang, String key, Object... args) {
        String pattern = Objects.requireNonNullElse(messageSource.getMessage(key, null, key, lang.locale()), key).replace("\\n", "\n");
        return String.format(pattern, args);
    }

    /** Russian plural form for age/year words. */
    public static String yearsRu(Lang lang, int n) {
        if (lang != Lang.RU) return String.valueOf(n);
        int mod100 = n % 100;
        int mod10  = n % 10;
        String form;
        if (mod100 >= 11 && mod100 <= 19)          form = get(lang, YEARS_OLD_MANY);
        else if (mod10 == 1)                        form = get(lang, YEARS_OLD_ONE);
        else if (mod10 >= 2 && mod10 <= 4)          form = get(lang, YEARS_OLD_FEW);
        else                                        form = get(lang, YEARS_OLD_MANY);
        return n + " " + form;
    }
}
