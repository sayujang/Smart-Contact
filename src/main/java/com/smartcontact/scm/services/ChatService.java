package com.smartcontact.scm.services;

import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.entities.UserStatus;
import java.util.List;
import java.util.Map;

public interface ChatService {
    
    ChatMessage saveMessage(ChatMessage message);
    
    List<ChatMessage> getConversationHistory(String userId1, String userId2);
    
    void markMessageAsDelivered(String messageId);
    
    
    long getUnreadMessageCount(String userId);
    
    void updateUserStatus(String userId, UserStatus.Status status, String sessionId);
    
    UserStatus getUserStatus(String userId);
    
    boolean isUserOnline(String userId);
    List<User> getUnknownUsers(String currentUserId);
    boolean markMessagesAsSeen(String senderId, String receiverId);
    Map<String, Long> getUnreadCountsSplit(String userId);
    List<String> getSendersWithUnreadMessages(String receiverId);
}