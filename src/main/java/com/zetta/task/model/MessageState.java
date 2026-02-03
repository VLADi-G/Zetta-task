package com.zetta.task.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_state")
public class MessageState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String messageId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String currentState;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Version
    private Long version;
    
    public MessageState() {
    }
    
    public MessageState(Long id, String messageId, String currentState, LocalDateTime timestamp, Long version) {
        this.id = id;
        this.messageId = messageId;
        this.currentState = currentState;
        this.timestamp = timestamp;
        this.version = version;
    }
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        timestamp = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}
