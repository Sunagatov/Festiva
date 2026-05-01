package com.festiva.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user_sessions")
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class UserSession {
    
    @Id
    private long userId;
    
    private BotState state = BotState.IDLE;
    
    @Indexed(expireAfter = "1h")
    private LocalDateTime lastActivity;
    
    public void touch() {
        this.lastActivity = LocalDateTime.now();
    }
}
