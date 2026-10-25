package com.festiva.command;

import com.festiva.i18n.Lang;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageBuilderMenuTest {

    @Test
    void mainMenuShowsThisMonthInBothLanguages() {
        assertThat(MessageBuilder.mainMenu(Lang.EN).getKeyboard())
                .flatExtracting(row -> row)
                .extracting(button -> button.getText())
                .contains("📅 This month");

        assertThat(MessageBuilder.mainMenu(Lang.RU).getKeyboard())
                .flatExtracting(row -> row)
                .extracting(button -> button.getText())
                .contains("📅 Этот месяц");
    }

    @Test
    void thisMonthLabelsRouteToTheNewCommand() {
        assertThat(MessageBuilder.LABEL_TO_COMMAND)
                .containsEntry("📅 This month", "/thismonth")
                .containsEntry("📅 Этот месяц", "/thismonth");
    }
}
