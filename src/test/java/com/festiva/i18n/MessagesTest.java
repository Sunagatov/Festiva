package com.festiva.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

import com.festiva.i18n.MessagesTestSupport;

@DisplayName("Messages")
class MessagesTest extends MessagesTestSupport {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Lang.class)
    @DisplayName("get() — every key resolves to a non-blank string for both languages")
    void get_allKeysResolveForBothLangs(Lang lang) {
        for (String key : new String[]{
                Messages.WELCOME, Messages.HELP, Messages.ENTER_NAME, Messages.ENTER_DATE,
                Messages.NAME_EMPTY, Messages.NAME_TOO_LONG, Messages.NAME_EXISTS, Messages.DATE_FORMAT_ERROR,
                Messages.DATE_FUTURE_ERROR, Messages.FRIEND_ADDED, Messages.FRIEND_NOT_FOUND,
                Messages.FRIEND_REMOVED, Messages.FRIENDS_EMPTY, Messages.CANCEL_ACTIVE,
                Messages.CANCEL_IDLE, Messages.UNKNOWN_COMMAND, Messages.NOTIFY_TODAY,
                Messages.NOTIFY_TOMORROW, Messages.NOTIFY_WEEK
        }) {
            assertThat(Messages.get(lang, key))
                    .as("key=%s lang=%s", key, lang)
                    .isNotBlank()
                    .isNotEqualTo(key); // key itself means missing entry
        }
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
}
