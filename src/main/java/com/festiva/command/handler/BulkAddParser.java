package com.festiva.command.handler;

import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BulkAddParser {

    public static final int MAX_ENTRIES = 50;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);

    private BulkAddParser() {}

    public record ParseResult(List<Friend> valid, List<String> errors) {}

    public static ParseResult parse(List<String> lines, Set<String> existingNames, Lang lang) {
        List<Friend> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        List<String> rows = lines.stream()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();

        if (rows.isEmpty()) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_NO_DATA));
            return new ParseResult(valid, errors);
        }

        int start = rows.getFirst().toLowerCase(Locale.ROOT).contains("name") ? 1 : 0;
        List<String> data = rows.subList(start, rows.size());

        if (data.size() > MAX_ENTRIES) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_TOO_MANY, MAX_ENTRIES, MAX_ENTRIES));
            data = data.subList(0, MAX_ENTRIES);
        }

        for (int i = 0; i < data.size(); i++) {
            String line = data.get(i);
            String[] parts = line.split(",", 2);
            int lineNum = start + i + 1;

            if (parts.length < 2) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_FORMAT, lineNum));
                continue;
            }

            String name = parts[0].trim();
            if (name.startsWith("\"") && name.endsWith("\""))
                name = name.substring(1, name.length() - 1).replace("\"\"", "\"").trim();
            String dateStr = parts[1].trim();

            if (name.isBlank()) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_NAME_EMPTY, lineNum));
                continue;
            }
            if (name.length() > 100) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_NAME_LONG, lineNum));
                continue;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, FMT);
            } catch (DateTimeParseException e) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DATE_INVALID, lineNum, name, dateStr));
                continue;
            }

            if (date.isAfter(LocalDate.now())) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DATE_FUTURE, lineNum, name));
                continue;
            }

            String nameLower = name.toLowerCase(Locale.ROOT);
            if (existingNames.contains(nameLower)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_EXISTS, lineNum, name));
                continue;
            }
            if (seenInBatch.contains(nameLower)) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DUPLICATE, lineNum, name));
                continue;
            }

            seenInBatch.add(nameLower);
            valid.add(new Friend(name, date));
        }

        return new ParseResult(valid, errors);
    }
}