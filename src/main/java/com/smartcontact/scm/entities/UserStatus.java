package com.smartcontact.scm.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "user_status")
public class UserStatus {
    
    @Id
    private String userId;
    private Status status;
    private LocalDateTime lastSeen;
    private String sessionId;
    
    public UserStatus() {
        this.lastSeen = LocalDateTime.now();
        this.status = Status.OFFLINE;
    }
    
    public UserStatus(String userId, Status status) {
        this();
        this.userId = userId;
        this.status = status;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.OFFLINE) {
            this.lastSeen = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public enum Status {
        ONLINE, OFFLINE
    }
}