package com.festiva.friend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;

@Data
@Document(collection = "friends")
@NoArgsConstructor
@CompoundIndex(name = "user_normalized_name", def = "{'telegramUserId': 1, 'normalizedName': 1}", unique = true)
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    private String normalizedName;  // lowercase trimmed for uniqueness
    
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
        // Validate name is not blank
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        
        this.name = name;
        this.normalizedName = normalizeName(name);
        
        // VALIDATE using Java's built-in types (throws DateTimeException if invalid)
        try {
            if (year != null) {
                @SuppressWarnings("unused")
                LocalDate validDate = LocalDate.of(year, month, day);
            } else {
                @SuppressWarnings("unused")
                MonthDay validMonthDay = MonthDay.of(month, day);
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date: " + 
                (year != null ? year + "-" : "") + month + "-" + day, e);
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
    
    // Normalize name for uniqueness checks
    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
    
    // Update normalized name when name changes
    public void setName(String name) {
        this.name = name;
        this.normalizedName = normalizeName(name);
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
        boolean isLeapDayBirthday = (birthMonth == 2 && birthDay == 29);
        
        // Try to create the birthday in the current year
        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            // Feb 29 in non-leap year → use Feb 28 if year is known, otherwise skip to next leap year
            if (hasYear()) {
                // With year: skip to next leap year
                int year = from.getYear();
                while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                    year++;
                }
                next = LocalDate.of(year, 2, 29);
            } else {
                // Without year: use Feb 28 in non-leap years
                next = LocalDate.of(from.getYear(), 2, 28);
            }
        }
        
        // If already passed this year, move to next occurrence
        if (next.isBefore(from)) {
            if (isLeapDayBirthday) {
                if (hasYear()) {
                    // With year: find next leap year
                    int year = from.getYear() + 1;
                    while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                        year++;
                    }
                    next = LocalDate.of(year, 2, 29);
                } else {
                    // Without year: try next year, use Feb 28 if not leap
                    int year = from.getYear() + 1;
                    next = LocalDate.of(year, 1, 1).isLeapYear() 
                        ? LocalDate.of(year, 2, 29) 
                        : LocalDate.of(year, 2, 28);
                }
            } else {
                next = LocalDate.of(from.getYear() + 1, birthMonth, birthDay);
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
