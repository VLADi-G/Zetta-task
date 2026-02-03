package com.zetta.task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "state_delta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
