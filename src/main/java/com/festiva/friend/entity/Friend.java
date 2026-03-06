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

    public Friend(String name, LocalDate birthDate) {
        this.name = name;
        this.birthDate = birthDate;
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
        return Period.between(birthDate, nextBirthday(today)).getYears();
    }
}
