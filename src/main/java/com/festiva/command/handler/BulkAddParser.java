package com.festiva.command.handler;

import com.festiva.friend.entity.Friend;
import com.festiva.friend.entity.Relationship;
import com.festiva.i18n.Lang;
import com.festiva.i18n.Messages;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
public final class BulkAddParser {

    public static final int MAX_ENTRIES = 50;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);

    private BulkAddParser() {}

    public record ParseResult(List<Friend> valid, List<String> errors, boolean noData) {}
    
    private record DateParseResult(Integer year, int month, int day) {}

    public static ParseResult parse(List<String> lines, Set<String> existingNames, Lang lang) {
        List<Friend> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        List<String> rows = lines.stream().map(String::trim).filter(l -> !l.isBlank()).toList();
        if (rows.isEmpty()) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_NO_DATA));
            return new ParseResult(valid, errors, true);
        }

        int start = rows.getFirst().toLowerCase(Locale.ROOT).contains("name") ? 1 : 0;
        List<String> data = rows.subList(start, rows.size());
        if (data.isEmpty()) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_NO_DATA));
            return new ParseResult(valid, errors, true);
        }
        if (data.size() > MAX_ENTRIES) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_TOO_MANY, data.size(), MAX_ENTRIES, MAX_ENTRIES));
            data = data.subList(0, MAX_ENTRIES);
        }

        for (int i = 0; i < data.size(); i++) {
            parseRow(data.get(i), start + i + 1, existingNames, seenInBatch, lang, valid, errors);
        }
        return new ParseResult(valid, errors, false);
    }

    private static void parseRow(String line, int lineNum, Set<String> existingNames,
                                  Set<String> seenInBatch, Lang lang,
                                  List<Friend> valid, List<String> errors) {
        String[] parts = line.split(",", 3);
        if (parts.length < 2) {
            errors.add(Messages.get(lang, Messages.BULK_ERROR_FORMAT, lineNum));
            return;
        }

        String name = parts[0].trim();
        if (name.startsWith("\"") && name.endsWith("\""))
            name = name.substring(1, name.length() - 1).replace("\"\"", "\"").trim();
        String dateStr = parts[1].trim();
        String relStr  = parts.length > 2 ? parts[2].trim() : "";

        String nameError = validateName(name, lineNum, existingNames, seenInBatch, lang);
        if (nameError != null) { errors.add(nameError); return; }

        DateParseResult dateResult = parseDate(dateStr, name, lineNum, lang, errors);
        if (dateResult == null) return;

        RelationshipParseResult relResult = parseRelationship(relStr, name, lineNum, lang);
        if (relResult.warning() != null) errors.add(relResult.warning());
        seenInBatch.add(name.toLowerCase(Locale.ROOT));
        valid.add(new Friend(name, dateResult.year(), dateResult.month(), dateResult.day(), relResult.relationship()));
    }

    private static String validateName(String name, int lineNum, Set<String> existingNames,
                                        Set<String> seenInBatch, Lang lang) {
        if (name.isBlank())          return Messages.get(lang, Messages.BULK_ERROR_NAME_EMPTY, lineNum);
        if (name.length() > 100)     return Messages.get(lang, Messages.BULK_ERROR_NAME_LONG, lineNum);
        String lower = name.toLowerCase(Locale.ROOT);
        if (existingNames.contains(lower)) return Messages.get(lang, Messages.BULK_ERROR_EXISTS, lineNum, name);
        if (seenInBatch.contains(lower))   return Messages.get(lang, Messages.BULK_ERROR_DUPLICATE, lineNum, name);
        return null;
    }

    private static DateParseResult parseDate(String dateStr, String name, int lineNum, Lang lang, List<String> errors) {
        try {
            // Check if year is missing (format: DD.MM. or DD.MM)
            if (dateStr.endsWith(".") || dateStr.matches("\\d{2}\\.\\d{2}$")) {
                // Parse as DD.MM without year
                String normalized = dateStr.endsWith(".") ? dateStr.substring(0, dateStr.length() - 1) : dateStr;
                String[] parts = normalized.split("\\.");
                if (parts.length == 2) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    // Validate using MonthDay
                    java.time.MonthDay.of(month, day);
                    return new DateParseResult(null, month, day);
                }
            }
            
            // Parse full date with year
            LocalDate date = LocalDate.parse(dateStr, FMT);
            if (date.isAfter(LocalDate.now())) {
                errors.add(Messages.get(lang, Messages.BULK_ERROR_DATE_FUTURE, lineNum, name));
                return null;
            }
            return new DateParseResult(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        } catch (Exception e) {
            log.debug("bulk.parse.date.invalid: line={}, value={}", lineNum, dateStr, e);
            errors.add(Messages.get(lang, Messages.BULK_ERROR_DATE_INVALID, lineNum, name, dateStr));
            return null;
        }
    }

    private record RelationshipParseResult(Relationship relationship, String warning) {}

    private static RelationshipParseResult parseRelationship(String relStr, String name, int lineNum, Lang lang) {
        if (relStr.isBlank()) return new RelationshipParseResult(null, null);
        try {
            return new RelationshipParseResult(Relationship.valueOf(relStr.toUpperCase(Locale.ROOT)), null);
        } catch (IllegalArgumentException e) {
            log.debug("bulk.parse.unknown.relationship: line={}, value={}", lineNum, relStr, e);
            return new RelationshipParseResult(null,
                    Messages.get(lang, Messages.BULK_ERROR_RELATIONSHIP_INVALID, lineNum, name, relStr));
        }
    }
}