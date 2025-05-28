// ChatMessageRepository.java
package com.selbuy.repository;

import com.selbuy.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByLotIdOrderByCreatedAtAsc(Long lotId);
    void deleteByLotId(Long lotId);
}