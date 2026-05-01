package com.festiva.importing;

import com.festiva.friend.entity.Friend;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "pending_imports")
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class PendingIcsImport {

    @Id
    private String id;

    @Indexed
    private long userId;

    private List<Friend> friends;

    @Indexed(expireAfter = "1h")
    private LocalDateTime createdAt;

    public PendingIcsImport(long userId, List<Friend> friends) {
        this.userId = userId;
        this.friends = friends;
        this.createdAt = LocalDateTime.now();
    }
}
