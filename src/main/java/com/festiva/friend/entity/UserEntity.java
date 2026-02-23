package com.festiva.friend.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "users")
public class UserEntity {

    @Id
    private String id;
    private long telegramUserId;
    private List<Friend> friends = new ArrayList<>();
}
