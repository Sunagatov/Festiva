package com.festiva.friend.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "friend_workflow_sessions")
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class FriendWorkflowSession {

    @Id
    private long userId;

    private String pendingName;
    private String pendingId;
    private Integer pendingYear;
    private Integer pendingMonth;
    private Integer pendingDay;
    private int yearPageOffset;

    @Indexed(expireAfter = "1h")
    private LocalDateTime lastActivity;

    public void touch() {
        this.lastActivity = LocalDateTime.now();
    }
}
