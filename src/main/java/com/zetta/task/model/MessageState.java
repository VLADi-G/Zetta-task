package com.zetta.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        timestamp = LocalDateTime.now();
    }
}
