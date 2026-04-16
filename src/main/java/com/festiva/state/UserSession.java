package com.festiva.state;

import com.festiva.friend.entity.Friend;
import com.festiva.i18n.Lang;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "user_sessions")
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    
    @Id
    private long userId;
    
    private BotState state = BotState.IDLE;
    private String pendingName;
    private String pendingId;
    private Integer pendingYear;
    private Integer pendingMonth;
    private Integer pendingDay;
    private int yearPageOffset = 0;
    private Lang lang;
    private List<Friend> pendingIcsImport;
    
    @Indexed(expireAfter = "1h")
    private LocalDateTime lastActivity;
    
    public void touch() {
        this.lastActivity = LocalDateTime.now();
    }
}
