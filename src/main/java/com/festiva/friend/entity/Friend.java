package com.festiva.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.Period;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class Friend {

    @Id
    private String id;
    private String name;
    private LocalDate birthDate;

    public Friend(String name, LocalDate birthDate) {
        this.name = name;
        this.birthDate = birthDate;
    }

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public int getNextAge() {
        return getAge() + 1;
    }
}
