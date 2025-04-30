package com.sapp.social.service;


import com.sapp.social.dto.MessageResponse;
import com.sapp.social.model.Message;
import com.sapp.social.model.User;
import com.sapp.social.repository.MessageRepository;
import com.sapp.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Message saveMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setStatus(Message.MessageStatus.SENT);

        return messageRepository.save(message);
    }

    public List<MessageResponse> getConversation(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = messageRepository.findConversation(user1, user2);

        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    public List<User> getChatUsers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> senders = messageRepository.findDistinctSendersByReceiver(user);
        List<User> receivers = messageRepository.findDistinctReceiversBySender(user);

        List<User> allChatUsers = new ArrayList<>(senders);
        receivers.forEach(r -> {
            if (!allChatUsers.contains(r)) {
                allChatUsers.add(r);
            }
        });

        return allChatUsers;
    }

    public void markAsDelivered(Long messageId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);

        messageOpt.ifPresent(message -> {
            message.setStatus(Message.MessageStatus.DELIVERED);
            messageRepository.save(message);
        });
    }

    public void markAsRead(Long messageId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);

        messageOpt.ifPresent(message -> {
            message.setStatus(Message.MessageStatus.READ);
            messageRepository.save(message);
        });
    }

    private MessageResponse convertToMessageResponse(Message message) {
        return new MessageResponse(
                message.getMessageId(),
                message.getSender().getUserId(),
                message.getSender().getUserName(),
                message.getReceiver().getUserId(),
                message.getReceiver().getUserName(),
                message.getContent(),
                message.getTimestamp()
        );
    }
}
