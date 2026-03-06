package com.festiva.command.handler;

import com.festiva.friend.entity.Friend;

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

    /**
     * Parses lines of "Name, DD.MM.YYYY" (text paste or CSV rows).
     * Skips header row if first line contains "name" (case-insensitive).
     */
    public static ParseResult parse(List<String> lines, Set<String> existingNames) {
        List<Friend> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        List<String> rows = lines.stream()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();

        if (rows.isEmpty()) {
            errors.add("No data found.");
            return new ParseResult(valid, errors);
        }

        // skip header if first row looks like a header
        int start = rows.getFirst().toLowerCase(Locale.ROOT).contains("name") ? 1 : 0;
        List<String> data = rows.subList(start, rows.size());

        if (data.size() > MAX_ENTRIES) {
            errors.add("Too many entries (max " + MAX_ENTRIES + "). Only first " + MAX_ENTRIES + " processed.");
            data = data.subList(0, MAX_ENTRIES);
        }

        for (int i = 0; i < data.size(); i++) {
            String line = data.get(i);
            String[] parts = line.split(",", 2);
            int lineNum = start + i + 1;

            if (parts.length < 2) {
                errors.add("Line " + lineNum + ": invalid format — expected \"Name, DD.MM.YYYY\"");
                continue;
            }

            String name = parts[0].trim();
            if (name.startsWith("\"") && name.endsWith("\"")) name = name.substring(1, name.length() - 1).trim();
            String dateStr = parts[1].trim();

            if (name.isBlank()) {
                errors.add("Line " + lineNum + ": name is empty");
                continue;
            }
            if (name.length() > 100) {
                errors.add("Line " + lineNum + ": name too long");
                continue;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, FMT);
            } catch (DateTimeParseException e) {
                errors.add("Line " + lineNum + " (" + name + "): invalid date \"" + dateStr + "\" — use DD.MM.YYYY");
                continue;
            }

            if (date.isAfter(LocalDate.now())) {
                errors.add("Line " + lineNum + " (" + name + "): birth date cannot be in the future");
                continue;
            }

            String nameLower = name.toLowerCase(Locale.ROOT);
            if (existingNames.contains(nameLower)) {
                errors.add("Line " + lineNum + " (" + name + "): already exists");
                continue;
            }
            if (seenInBatch.contains(nameLower)) {
                errors.add("Line " + lineNum + " (" + name + "): duplicate in this batch");
                continue;
            }

            seenInBatch.add(nameLower);
            valid.add(new Friend(name, date));
        }

        return new ParseResult(valid, errors);
    }
}
