package com.festiva.command;

import com.festiva.command.handler.BulkAddParser;
import com.festiva.command.handler.BulkAddParser.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BulkAddParser")
class BulkAddParserTest {

    @Test
    @DisplayName("valid rows without header → all parsed correctly")
    void validRows_noParsedCorrectly() {
        ParseResult r = BulkAddParser.parse(List.of("Alice,15.03.1990", "Bob,22.07.1985"), Set.of());

        assertThat(r.valid()).hasSize(2);
        assertThat(r.errors()).isEmpty();
        assertThat(r.valid().getFirst().getName()).isEqualTo("Alice");
        assertThat(r.valid().getFirst().getBirthDate()).isEqualTo(LocalDate.of(1990, 3, 15));
        assertThat(r.valid().get(1).getName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("first row contains 'name' → header is skipped")
    void headerRow_isSkipped() {
        ParseResult r = BulkAddParser.parse(List.of("name,birthday", "Alice,15.03.1990"), Set.of());

        assertThat(r.valid()).hasSize(1);
        assertThat(r.valid().getFirst().getName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("header detection is case-insensitive")
    void headerRow_caseInsensitive() {
        ParseResult r = BulkAddParser.parse(List.of("Name,Birthday", "Bob,22.07.1985"), Set.of());

        assertThat(r.valid()).hasSize(1);
    }

    @Test
    @DisplayName("blank lines are ignored")
    void blankLines_areIgnored() {
        ParseResult r = BulkAddParser.parse(List.of("Alice,15.03.1990", "  ", "", "Bob,22.07.1985"), Set.of());

        assertThat(r.valid()).hasSize(2);
        assertThat(r.errors()).isEmpty();
    }

    @Test
    @DisplayName("name already in existing set → rejected with 'already exists' error")
    void existingName_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of("Alice,15.03.1990"), Set.of("alice"));

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("already exists");
    }

    @Test
    @DisplayName("duplicate name within batch → second occurrence rejected")
    void duplicateInBatch_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of("Alice,15.03.1990", "Alice,01.01.2000"), Set.of());

        assertThat(r.valid()).hasSize(1);
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("duplicate");
    }

    @Test
    @DisplayName("date not in DD.MM.YYYY format → rejected with 'invalid date' error")
    void invalidDateFormat_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of("Alice,1990-03-15"), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("invalid date");
    }

    @Test
    @DisplayName("birth date in the future → rejected")
    void futureDate_isRejected() {
        String future = LocalDate.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT));
        ParseResult r = BulkAddParser.parse(List.of("Alice," + future), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("future");
    }

    @Test
    @DisplayName("row without comma → rejected with 'invalid format' error")
    void missingComma_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of("Alice 15.03.1990"), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("invalid format");
    }

    @Test
    @DisplayName("empty name before comma → rejected with 'name is empty' error")
    void emptyName_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of(",15.03.1990"), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("name is empty");
    }

    @Test
    @DisplayName("name longer than 100 characters → rejected with 'name too long' error")
    void nameTooLong_isRejected() {
        ParseResult r = BulkAddParser.parse(List.of("A".repeat(101) + ",15.03.1990"), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("name too long");
    }

    @Test
    @DisplayName("more than MAX_ENTRIES rows → only first MAX_ENTRIES processed, error added")
    void exceedsMaxEntries_capsAndAddsError() {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= BulkAddParser.MAX_ENTRIES + 5; i++) {
            lines.add("Person" + i + ",01.01.1990");
        }
        ParseResult r = BulkAddParser.parse(lines, Set.of());

        assertThat(r.valid()).hasSize(BulkAddParser.MAX_ENTRIES);
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().getFirst()).contains("Too many entries");
    }

    @Test
    @DisplayName("empty input list → returns single error")
    void emptyInput_returnsError() {
        ParseResult r = BulkAddParser.parse(List.of(), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
    }

    @Test
    @DisplayName("only blank lines → returns single error")
    void onlyBlankLines_returnsError() {
        ParseResult r = BulkAddParser.parse(List.of("  ", "\t", ""), Set.of());

        assertThat(r.valid()).isEmpty();
        assertThat(r.errors()).hasSize(1);
    }

    @Test
    @DisplayName("mix of valid and invalid rows → valid saved, errors collected")
    void mixedRows_validSavedErrorsCollected() {
        ParseResult r = BulkAddParser.parse(List.of(
                "Alice,15.03.1990",
                "bad-line",
                "Bob,22.07.1985",
                ",01.01.2000"
        ), Set.of());

        assertThat(r.valid()).hasSize(2);
        assertThat(r.errors()).hasSize(2);
    }
}
