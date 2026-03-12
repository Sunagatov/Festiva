package com.festiva.friend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;

@Data
@Document(collection = "friends")
@NoArgsConstructor
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    
    // New fields replacing birthDate
    private Integer birthYear;    // Nullable (null = year unknown)
    private int birthMonth;       // 1-12 (always set)
    private int birthDay;         // 1-31 (always set)
    
    private boolean notifyEnabled = true;
    private Relationship relationship;

    // Constructor for full date (with year)
    public Friend(String name, LocalDate birthDate) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
    }

    public Friend(String name, LocalDate birthDate, Relationship relationship) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
        this.relationship = relationship;
    }
    
    // Constructor with validation (supports optional year)
    public Friend(String name, Integer year, int month, int day) {
        this.name = name;
        
        // VALIDATE using Java's built-in types
        if (year != null) {
            LocalDate.of(year, month, day);  // Validates full date (throws if invalid)
        } else {
            MonthDay.of(month, day);  // Validates month+day only (throws if invalid)
        }
        
        // Only store if validation passed
        this.birthYear = year;
        this.birthMonth = month;
        this.birthDay = day;
    }
    
    public Friend(String name, Integer year, int month, int day, Relationship relationship) {
        this(name, year, month, day);
        this.relationship = relationship;
    }
    
    // Helper methods
    public boolean hasYear() {
        return birthYear != null;
    }
    
    public MonthDay getBirthMonthDay() {
        return MonthDay.of(birthMonth, birthDay);
    }
    
    public LocalDate getBirthDate() {
        if (!hasYear()) {
            throw new IllegalStateException("Birth year is unknown for " + name);
        }
        return LocalDate.of(birthYear, birthMonth, birthDay);
    }

    public int getAge(LocalDate on) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return Period.between(getBirthDate(), on).getYears();
    }

    public LocalDate nextBirthday(LocalDate from) {
        // Special handling for Feb 29 when year is known
        if (hasYear() && birthMonth == 2 && birthDay == 29) {
            // Find next leap year from current date
            int year = from.getYear();
            if (!java.time.Year.isLeap(year) || LocalDate.of(year, 2, 29).isBefore(from)) {
                year++;
                while (!java.time.Year.isLeap(year)) {
                    year++;
                }
            }
            return LocalDate.of(year, 2, 29);
        }
        
        // Try to create the birthday in the current year
        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            // Feb 29 in non-leap year (when year is unknown) → use Feb 28
            next = LocalDate.of(from.getYear(), 2, 28);
        }
        
        // If already passed this year, move to next year
        if (next.isBefore(from)) {
            try {
                next = LocalDate.of(from.getYear() + 1, birthMonth, birthDay);
            } catch (DateTimeException e) {
                next = LocalDate.of(from.getYear() + 1, 2, 28);
            }
        }
        
        return next;
    }

    public int getNextAge(LocalDate from) {
        if (!hasYear()) {
            throw new IllegalStateException("Cannot calculate age without birth year for " + name);
        }
        return nextBirthday(from).getYear() - birthYear;
    }

    private static final int[][] ZODIAC_ENDS = {{1,19},{2,18},{3,20},{4,19},{5,20},{6,20},{7,22},{8,22},{9,22},{10,22},{11,21},{12,21}};
    private static final String[] ZODIAC_SIGNS = {"♑","♒","♓","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑"};

    public String getZodiac() {
        int m = birthMonth;
        int d = birthDay;
        int idx = d <= ZODIAC_ENDS[m - 1][1] ? m - 1 : m;
        return ZODIAC_SIGNS[idx];
    }
}
