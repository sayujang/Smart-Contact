package com.smartcontact.scm.services.implementation;

import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.UserStatus;
import com.smartcontact.scm.repositories.ChatMessageRepository;
import com.smartcontact.scm.repositories.UserStatusRepository;
import com.smartcontact.scm.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserStatusRepository userStatusRepository;
    
    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        message.setStatus(ChatMessage.MessageStatus.SENT);
        return chatMessageRepository.save(message);
    }
    
    @Override
    public List<ChatMessage> getConversationHistory(String userId1, String userId2) {
        // FIX: Call the new query method
        // We only need to pass the two IDs once. The Query handles the "OR" logic.
        return chatMessageRepository.findByChatHistory(userId1, userId2);
    }
    
    // ... keep the rest of your methods exactly as they are ...
    // (markMessageAsDelivered, markMessageAsRead, getUnreadMessageCount, etc.)
    
    @Override
    public void markMessageAsDelivered(String messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(ChatMessage.MessageStatus.DELIVERED);
            chatMessageRepository.save(message);
        });
    }

    @Override
    public void markMessageAsRead(String messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(ChatMessage.MessageStatus.READ);
            chatMessageRepository.save(message);
        });
    }

    @Override
    public long getUnreadMessageCount(String userId) {
        return chatMessageRepository.countByReceiverIdAndStatus(userId, ChatMessage.MessageStatus.SENT);
    }

    @Override
    public void updateUserStatus(String userId, UserStatus.Status status, String sessionId) {
         UserStatus userStatus = userStatusRepository.findByUserId(userId)
            .orElse(new UserStatus(userId, status));
        
        userStatus.setStatus(status);
        userStatus.setSessionId(sessionId);
        
        if (status == UserStatus.Status.OFFLINE) {
            userStatus.setLastSeen(LocalDateTime.now());
        }
        
        userStatusRepository.save(userStatus);
    }

    @Override
    public UserStatus getUserStatus(String userId) {
         return userStatusRepository.findByUserId(userId)
            .orElse(new UserStatus(userId, UserStatus.Status.OFFLINE));
    }
    @Override
public boolean isUserOnline(String userId) {
    return userStatusRepository.findByUserId(userId)
        .map(status -> status.getStatus() == UserStatus.Status.ONLINE)
        .orElse(false);
}
}