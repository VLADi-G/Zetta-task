package com.zetta.task.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "state_delta")
public class StateDelta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long messageStateId;
    
    @Column(columnDefinition = "TEXT")
    private String beforeState;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String afterState;
    
    @Column(columnDefinition = "TEXT")
    private String changes;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    public StateDelta() {
    }
    
    public StateDelta(Long id, Long messageStateId, String beforeState, String afterState, String changes, LocalDateTime timestamp) {
        this.id = id;
        this.messageStateId = messageStateId;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.changes = changes;
        this.timestamp = timestamp;
    }
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getMessageStateId() {
        return messageStateId;
    }
    
    public void setMessageStateId(Long messageStateId) {
        this.messageStateId = messageStateId;
    }
    
    public String getBeforeState() {
        return beforeState;
    }
    
    public void setBeforeState(String beforeState) {
        this.beforeState = beforeState;
    }
    
    public String getAfterState() {
        return afterState;
    }
    
    public void setAfterState(String afterState) {
        this.afterState = afterState;
    }
    
    public String getChanges() {
        return changes;
    }
    
    public void setChanges(String changes) {
        this.changes = changes;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
