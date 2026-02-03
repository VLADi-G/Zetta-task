package com.zetta.task.repository;

import com.zetta.task.model.MessageState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageStateRepository extends JpaRepository<MessageState, Long> {
    Optional<MessageState> findByMessageId(String messageId);
}
