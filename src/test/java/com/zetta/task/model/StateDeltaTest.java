package com.zetta.task.model;

import com.zetta.task.repository.StateDeltaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StateDeltaTest {
    
    @Autowired
    private StateDeltaRepository stateDeltaRepository;
    
    @Test
    void testSaveStateDelta_PersistsSuccessfully() {
        // Given
        StateDelta delta = StateDelta.builder()
                .messageStateId(1L)
                .beforeState("{\"age\": 25}")
                .afterState("{\"age\": 26}")
                .changes("Age incremented by 1")
                .build();
        
        // When
        StateDelta saved = stateDeltaRepository.save(delta);
        
        // Then
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getMessageStateId());
        assertEquals("{\"age\": 25}", saved.getBeforeState());
        assertEquals("{\"age\": 26}", saved.getAfterState());
        assertEquals("Age incremented by 1", saved.getChanges());
        assertNotNull(saved.getTimestamp());
    }
    
    @Test
    void testFindByMessageStateId_ReturnsCorrectDeltas() {
        // Given
        StateDelta delta1 = StateDelta.builder()
                .messageStateId(1L)
                .beforeState("{\"age\": 25}")
                .afterState("{\"age\": 26}")
                .changes("First change")
                .build();
        
        StateDelta delta2 = StateDelta.builder()
                .messageStateId(1L)
                .beforeState("{\"age\": 26}")
                .afterState("{\"age\": 27}")
                .changes("Second change")
                .build();
        
        stateDeltaRepository.save(delta1);
        stateDeltaRepository.save(delta2);
        
        // When
        var deltas = stateDeltaRepository.findByMessageStateIdOrderByTimestampAsc(1L);
        
        // Then
        assertEquals(2, deltas.size());
        assertEquals("First change", deltas.get(0).getChanges());
        assertEquals("Second change", deltas.get(1).getChanges());
    }
}
