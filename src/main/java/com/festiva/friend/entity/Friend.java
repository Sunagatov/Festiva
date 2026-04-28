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
import java.util.Locale;

@Data
@Document(collection = "friends")
@NoArgsConstructor
@CompoundIndex(name = "user_normalized_name", def = "{'telegramUserId': 1, 'normalizedName': 1}", unique = true)
@SuppressWarnings("unused")
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    private String normalizedName;

    private Integer birthYear;
    private int birthMonth;
    private int birthDay;

    private boolean notifyEnabled = true;
    private Relationship relationship;

    public Friend(String name, LocalDate birthDate) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
    }

    public Friend(String name, LocalDate birthDate, Relationship relationship) {
        this(name, birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
        this.relationship = relationship;
    }

    public Friend(String name, Integer year, int month, int day) {
        String sanitizedName = name == null ? null : name.trim();
        if (sanitizedName == null || sanitizedName.isBlank()) {
            throw new IllegalArgumentException("Friend name cannot be blank");
        }
        if (sanitizedName.length() > 100) {
            throw new IllegalArgumentException("Friend name cannot be longer than 100 characters");
        }

        this.name = sanitizedName;
        this.normalizedName = normalizeName(sanitizedName);

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

        this.birthYear = year;
        this.birthMonth = month;
        this.birthDay = day;
    }

    public Friend(String name, Integer year, int month, int day, Relationship relationship) {
        this(name, year, month, day);
        this.relationship = relationship;
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
        this.normalizedName = normalizeName(this.name);
    }

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

        LocalDate next;
        try {
            next = LocalDate.of(from.getYear(), birthMonth, birthDay);
        } catch (DateTimeException e) {
            if (hasYear()) {
                int year = from.getYear();
                while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                    year++;
                }
                next = LocalDate.of(year, 2, 29);
            } else {
                next = LocalDate.of(from.getYear(), 2, 28);
            }
        }

        if (next.isBefore(from)) {
            if (isLeapDayBirthday) {
                if (hasYear()) {
                    int year = from.getYear() + 1;
                    while (!LocalDate.of(year, 1, 1).isLeapYear()) {
                        year++;
                    }
                    next = LocalDate.of(year, 2, 29);
                } else {
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
