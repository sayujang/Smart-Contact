package com.smartcontact.scm.services;

import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.UserStatus;
import java.util.List;

public interface ChatService {
    
    ChatMessage saveMessage(ChatMessage message);
    
    List<ChatMessage> getConversationHistory(String userId1, String userId2);
    
    void markMessageAsDelivered(String messageId);
    
    void markMessageAsRead(String messageId);
    
    long getUnreadMessageCount(String userId);
    
    void updateUserStatus(String userId, UserStatus.Status status, String sessionId);
    
    UserStatus getUserStatus(String userId);
    
    boolean isUserOnline(String userId);
}