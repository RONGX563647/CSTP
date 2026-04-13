package com.aisale.backend.repository;

import com.aisale.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySenderIdAndReceiverIdOrderByCreatedAtAsc(Long senderId, Long receiverId);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(Long userId1, Long userId2);

    @Query("SELECT DISTINCT m.receiverId FROM ChatMessage m WHERE m.senderId = ?1")
    List<Long> findChatPartners(Long userId);

    List<ChatMessage> findByReceiverIdAndIsReadFalse(Long receiverId);
}