package com.sapp.social.repository;

import com.sapp.social.model.Message;
import com.sapp.social.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.sender = ?1 AND m.receiver = ?2) OR (m.sender = ?2 AND m.receiver = ?1) ORDER BY m.messageId ASC")
    List<Message> findConversation(User sender, User receiver);

    List<Message> findBySenderOrderByMessageIdDesc(User sender);

    List<Message> findByReceiverOrderByMessageIdDesc(User receiver);

    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = ?1")
    List<User> findDistinctSendersByReceiver(User receiver);

    @Query("SELECT DISTINCT m.receiver FROM Message m WHERE m.sender = ?1")
    List<User> findDistinctReceiversBySender(User sender);
}
