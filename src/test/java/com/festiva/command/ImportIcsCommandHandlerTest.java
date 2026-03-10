package com.festiva.command;

import com.festiva.command.handler.ImportIcsCommandHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImportIcsCommandHandler — ICS parsing")
class ImportIcsCommandHandlerTest {

    private static List<String> extract(String... lines) {
        return ImportIcsCommandHandler.extractYearlyCsvLines(List.of(lines));
    }

    @Test
    @DisplayName("DTSTART;VALUE=DATE → parsed as DD.MM.YYYY")
    void dtstart_valueDate_parsed() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Alice",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Alice,15.03.1990");
    }

    @Test
    @DisplayName("DTSTART datetime format → date part extracted")
    void dtstart_datetime_parsed() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Bob",
                "DTSTART:19850722T000000Z",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Bob,22.07.1985");
    }

    @Test
    @DisplayName("event without RRULE:FREQ=YEARLY → silently skipped")
    void nonYearlyEvent_skipped() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Team Lunch",
                "DTSTART;VALUE=DATE:20240101",
                "RRULE:FREQ=WEEKLY",
                "END:VEVENT"
        );
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("event without RRULE → silently skipped")
    void noRrule_skipped() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:One-off",
                "DTSTART;VALUE=DATE:20240101",
                "END:VEVENT"
        );
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("event without SUMMARY → silently skipped")
    void noSummary_skipped() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("event without DTSTART → silently skipped")
    void noDtstart_skipped() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Alice",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("mixed yearly and non-yearly events → only yearly returned")
    void mixed_onlyYearlyReturned() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Alice",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT",
                "BEGIN:VEVENT",
                "SUMMARY:Team Lunch",
                "DTSTART;VALUE=DATE:20240601",
                "END:VEVENT",
                "BEGIN:VEVENT",
                "SUMMARY:Bob",
                "DTSTART;VALUE=DATE:19850722",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Alice,15.03.1990", "Bob,22.07.1985");
    }

    @Test
    @DisplayName("ICS line folding → unfolded before parsing")
    void lineFolding_unfolded() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMM",
                " ARY:Alice",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Alice,15.03.1990");
    }

    @Test
    @DisplayName("RRULE with extra params containing FREQ=YEARLY → accepted")
    void rruleWithExtraParams_accepted() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:Alice",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY;BYMONTH=3",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Alice,15.03.1990");
    }

    @Test
    @DisplayName("empty file → empty result")
    void emptyFile_returnsEmpty() {
        assertThat(extract()).isEmpty();
    }

    @Test
    @DisplayName("SUMMARY with leading/trailing whitespace → trimmed")
    void summary_trimmed() {
        List<String> result = extract(
                "BEGIN:VEVENT",
                "SUMMARY:  Alice  ",
                "DTSTART;VALUE=DATE:19900315",
                "RRULE:FREQ=YEARLY",
                "END:VEVENT"
        );
        assertThat(result).containsExactly("Alice,15.03.1990");
    }
}
