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
        StateDelta delta = new StateDelta();
        delta.setMessageStateId(1L);
        delta.setBeforeState("{\"age\": 25}");
        delta.setAfterState("{\"age\": 26}");
        delta.setChanges("Age incremented by 1");
        
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
        StateDelta delta1 = new StateDelta();
        delta1.setMessageStateId(1L);
        delta1.setBeforeState("{\"age\": 25}");
        delta1.setAfterState("{\"age\": 26}");
        delta1.setChanges("First change");
        
        StateDelta delta2 = new StateDelta();
        delta2.setMessageStateId(1L);
        delta2.setBeforeState("{\"age\": 26}");
        delta2.setAfterState("{\"age\": 27}");
        delta2.setChanges("Second change");
        
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
