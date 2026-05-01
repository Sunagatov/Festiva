package com.festiva.command;

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
    @DisplayName("yearKeyboard → year labels include age context and preserve callback")
    void yearKeyboard_includesAgeContext() {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - DatePickerKeyboard.DEFAULT_YEAR_OFFSET - 8 + 1;

        InlineKeyboardMarkup markup = DatePickerKeyboard.yearKeyboard(DatePickerKeyboard.DEFAULT_YEAR_OFFSET, Lang.EN);
        InlineKeyboardButton firstButton = markup.getKeyboard().getFirst().getFirst();

        assertThat(firstButton.getText()).isEqualTo(startYear + " · " + (currentYear - startYear) + "y");
        assertThat(firstButton.getCallbackData()).isEqualTo(DatePickerKeyboard.DATE_YEAR_PREFIX + startYear);
    }
}
