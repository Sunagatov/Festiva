package com.festiva.friend.handler;

import com.festiva.i18n.Lang;
import com.festiva.i18n.MessagesTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatePickerKeyboard")
@SuppressWarnings("unused")
class DatePickerKeyboardTest extends MessagesTestSupport {

    @Test
    @DisplayName("yearKeyboard first button starts at computed start year")
    void yearKeyboard_startsAtExpectedYear() {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - DatePickerKeyboard.DEFAULT_YEAR_OFFSET - 8 + 1;

        InlineKeyboardMarkup markup = DatePickerKeyboard.yearKeyboard(DatePickerKeyboard.DEFAULT_YEAR_OFFSET, Lang.EN);

        InlineKeyboardButton firstButton = markup.getKeyboard().getFirst().getFirst();
        assertThat(firstButton.getText()).startsWith(String.valueOf(startYear));
        assertThat(firstButton.getCallbackData()).isEqualTo(DatePickerKeyboard.DATE_YEAR_PREFIX + startYear);
    }
}
