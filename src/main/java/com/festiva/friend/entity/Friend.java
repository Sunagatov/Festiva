package com.festiva.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.Period;

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

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public LocalDate nextBirthday(LocalDate from) {
        LocalDate next = birthDate.withYear(from.getYear());
        return next.isBefore(from) ? next.plusYears(1) : next;
    }

    public int getNextAge() {
        LocalDate today = LocalDate.now();
        LocalDate next = nextBirthday(today);
        return next.getYear() - birthDate.getYear();
    }

    public String getZodiac() {
        int m = birthDate.getMonthValue(), d = birthDate.getDayOfMonth();
        if ((m == 3 && d >= 21) || (m == 4 && d <= 19)) return "♈";
        if ((m == 4 && d >= 20) || (m == 5 && d <= 20)) return "♉";
        if ((m == 5 && d >= 21) || (m == 6 && d <= 20)) return "♊";
        if ((m == 6 && d >= 21) || (m == 7 && d <= 22)) return "♋";
        if ((m == 7 && d >= 23) || (m == 8 && d <= 22)) return "♌";
        if ((m == 8 && d >= 23) || (m == 9 && d <= 22)) return "♍";
        if ((m == 9 && d >= 23) || (m == 10 && d <= 22)) return "♎";
        if ((m == 10 && d >= 23) || (m == 11 && d <= 21)) return "♏";
        if ((m == 11 && d >= 22) || (m == 12 && d <= 21)) return "♐";
        if ((m == 12 && d >= 22) || (m == 1 && d <= 19)) return "♑";
        if ((m == 1 && d >= 20) || (m == 2 && d <= 18)) return "♒";
        return "♓";
    }
}
