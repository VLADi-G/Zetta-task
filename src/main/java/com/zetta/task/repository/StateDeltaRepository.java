package com.zetta.task.repository;

import com.zetta.task.model.StateDelta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateDeltaRepository extends JpaRepository<StateDelta, Long> {
    List<StateDelta> findByMessageStateIdOrderByTimestampAsc(Long messageStateId);
}
