package com.festiva.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.Period;
import java.time.Year;

@Data
@Document(collection = "friends")
@NoArgsConstructor
@AllArgsConstructor
public class Friend {

    @Id
    private String id;
    @Indexed
    private long telegramUserId;
    private String name;
    private LocalDate birthDate;
    private boolean notifyEnabled = true;
    private Relationship relationship;

    public Friend(String name, LocalDate birthDate) {
        this.name = name;
        this.birthDate = birthDate;
    }

    public Friend(String name, LocalDate birthDate, Relationship relationship) {
        this.name = name;
        this.birthDate = birthDate;
        this.relationship = relationship;
    }

    public int getAge(LocalDate on) {
        return Period.between(birthDate, on).getYears();
    }

    public LocalDate nextBirthday(LocalDate from) {
        LocalDate next = birthDate.withYear(from.getYear());
        if (next.isBefore(from)) next = next.plusYears(1);
        if (birthDate.getMonthValue() == 2 && birthDate.getDayOfMonth() == 29 && next.getDayOfMonth() == 28) {
            while (!Year.isLeap(next.getYear())) next = next.plusYears(1);
            next = next.withDayOfMonth(29);
        }
        return next;
    }

    public int getNextAge(LocalDate from) {
        return nextBirthday(from).getYear() - birthDate.getYear();
    }

    private static final int[][] ZODIAC_ENDS = {{1,19},{2,18},{3,20},{4,19},{5,20},{6,20},{7,22},{8,22},{9,22},{10,22},{11,21},{12,21}};
    private static final String[] ZODIAC_SIGNS = {"♑","♒","♓","♈","♉","♊","♋","♌","♍","♎","♏","♐","♑"};

    public String getZodiac() {
        int m = birthDate.getMonthValue(), d = birthDate.getDayOfMonth();
        int idx = d <= ZODIAC_ENDS[m - 1][1] ? m - 1 : m;
        return ZODIAC_SIGNS[idx];
    }
}
