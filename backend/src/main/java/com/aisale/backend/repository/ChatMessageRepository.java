package com.aisale.backend.repository;

import com.aisale.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySenderIdAndReceiverIdOrderByCreatedAtAsc(Long senderId, Long receiverId);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(Long userId1, Long userId2);

    @Query("SELECT DISTINCT m.receiverId FROM ChatMessage m WHERE m.senderId = ?1")
    List<Long> findChatPartners(Long userId);

    List<ChatMessage> findByReceiverIdAndIsReadFalse(Long receiverId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.senderId = :partnerId AND m.receiverId = :currentUserId AND m.isRead = false")
    int countUnreadFromPartner(@Param("partnerId") Long partnerId, @Param("currentUserId") Long currentUserId);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessage(Long userId1, Long userId2, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END FROM ChatMessage m WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<Long> findAllPartnerIds(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :partnerId AND m.receiverId = :currentUserId AND m.isRead = false")
    void markConversationAsRead(@Param("partnerId") Long partnerId, @Param("currentUserId") Long currentUserId);
}
