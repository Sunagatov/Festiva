package com.festiva.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Messages")
@SuppressWarnings("unused")
class MessagesTest extends MessagesTestSupport {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Lang.class)
    @DisplayName("get() — every key resolves to a non-blank string for both languages")
    void get_allKeysResolveForBothLangs(Lang lang) {
        for (String key : new String[]{
                Messages.WELCOME, Messages.MENU, Messages.ENTER_NAME,
                Messages.NAME_EMPTY, Messages.NAME_TOO_LONG, Messages.NAME_EXISTS,
                Messages.DATE_FUTURE_ERROR, Messages.FRIEND_ADDED, Messages.FRIEND_NOT_FOUND,
                Messages.FRIEND_REMOVED, Messages.FRIENDS_EMPTY, Messages.CANCEL_ACTIVE,
                Messages.CANCEL_IDLE, Messages.UNKNOWN_COMMAND, Messages.NOTIFY_TODAY,
                Messages.NOTIFY_TOMORROW, Messages.NOTIFY_WEEK,
                Messages.NOTIFY_TODAY_NO_YEAR, Messages.NOTIFY_TOMORROW_NO_YEAR, Messages.NOTIFY_WEEK_NO_YEAR,
                Messages.ICS_CANCELLED, Messages.ICS_NONE_SAVED
        }) {
            assertThat(Messages.get(lang, key))
                    .as("key=%s lang=%s", key, lang)
                    .isNotBlank()
                    .isNotEqualTo(key); // key itself means missing entry
        }
    }

    @Test
    @DisplayName("ics_cancelled RU — must not contain format specifiers (merged-line regression)")
    void icsCancelled_ru_hasNoFormatSpecifiers() {
        String value = Messages.get(Lang.RU, Messages.ICS_CANCELLED);
        assertThat(value)
                .doesNotContain("%s")
                .doesNotContain("%d")
                .doesNotContain("notify_today_no_year");
    }

    @Test
    @DisplayName("get() with args — formats placeholders correctly")
    void get_withArgs_formatsCorrectly() {
        assertThat(Messages.get(Lang.EN, Messages.FRIEND_ADDED, "Alice")).contains("Alice");
        assertThat(Messages.get(Lang.RU, Messages.FRIEND_ADDED, "Alice")).contains("Alice");
    }

    @Test
    @DisplayName("get() — missing key returns the key itself as fallback")
    void get_missingKey_returnsFallback() {
        assertThat(Messages.get(Lang.EN, "no_such_key")).isEqualTo("no_such_key");
    }

    @Test
    @DisplayName("Lang.locale() — EN returns ENGLISH, RU returns ru locale")
    void lang_locale() {
        assertThat(Lang.EN.locale().getLanguage()).isEqualTo("en");
        assertThat(Lang.RU.locale().getLanguage()).isEqualTo("ru");
    }

    @Test
    @DisplayName("notify_today EN — uses warmer copy without relationship or zodiac fragments")
    void notifyToday_en_usesWarmCopy() {
        String value = Messages.get(Lang.EN, Messages.NOTIFY_TODAY, "Alice", "30", "festiva_bot");
        assertThat(value).isEqualTo(
                """
                🎂 <b>Happy Birthday!</b>
                <b>Alice</b> turns <b>30</b> today.
                💌 Don't forget to wish them well!
                👉 <a href="https://t.me/festiva_bot">Open Festiva</a>"""
        );
        assertThat(value)
                .doesNotContain("birthday day")
                .doesNotContain("Friend")
                .doesNotContain("Aries");
    }

    @Test
    @DisplayName("notify_today_no_year EN — says has a birthday today")
    void notifyTodayNoYear_en_usesBirthdayTodayCopy() {
        String value = Messages.get(Lang.EN, Messages.NOTIFY_TODAY_NO_YEAR, "Alice", "festiva_bot");
        assertThat(value).isEqualTo(
                """
                🎂 <b>Happy Birthday!</b>
                <b>Alice</b> has a birthday today.
                💌 Don't forget to wish them well!
                👉 <a href="https://t.me/festiva_bot">Open Festiva</a>"""
        );
        assertThat(value)
                .doesNotContain("celebrating")
                .doesNotContain("Friend")
                .doesNotContain("Aries");
    }
}
