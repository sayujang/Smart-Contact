package com.smartcontact.scm.controllers;

import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.UserStatus;
import com.smartcontact.scm.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    
    //sending messages
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        // Save to Database
        ChatMessage savedMessage = chatService.saveMessage(message);

        // 1. Send to receiver
        messagingTemplate.convertAndSend(
            "/queue/messages/" + message.getReceiverId(), 
            savedMessage
        );

        // 2. Send to sender (so it shows up on their screen immediately)
        messagingTemplate.convertAndSend(
            "/queue/messages/" + message.getSenderId(), 
            savedMessage
        );
    }


    //typing indicators
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, String> payload) {
        String receiverId = payload.get("receiverId");
        
        // Forward typing status directly to the receiver
        messagingTemplate.convertAndSend(
            "/queue/typing/" + receiverId,
            payload
        );
    }

    // It handles online status and saves the userId for disconnection events.
    @MessageMapping("/chat.connect")
    @SendTo("/topic/user.status") //doesn't add any prefix
    public Map<String, String> addUser(
            @Payload Map<String, String> data, 
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userId = data.get("userId");
        
        // Save User ID in the WebSocket Session
        // This allows the eventlistener (on the server) to know who disconnected later.
        if (userId != null) {
            headerAccessor.getSessionAttributes().put("userId", userId); //websockets are stateful (connection never closes like http connections) so a connection must be uniquely identified 
            
            // update db to Online
            chatService.updateUserStatus(userId, UserStatus.Status.ONLINE, headerAccessor.getSessionId());
        }
        
        // broadcast to everyone subscribed to /topic/user.status
        return Map.of("userId", userId, "status", "ONLINE");
    }

   @MessageMapping("/chat.disconnect")
   public void disconnectUser(@Payload Map<String, String> disconnectuserId)
   {
     String userId=disconnectuserId.get("userId");
     System.out.println("LOG: User " + userId + " disconnected intentionally(logged out).");
   }


    //API ENDPOINTS (For loading history via HTTP)
    @GetMapping("/api/chat/history/{userId1}/{userId2}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String userId1, @PathVariable String userId2) {
        return chatService.getConversationHistory(userId1, userId2);
    }

    @GetMapping("/api/chat/status/{userId}")
    @ResponseBody
    public Map<String, Object> getUserStatus(@PathVariable String userId) {
        UserStatus status = chatService.getUserStatus(userId);
        
        // Handle case where user has no status record yet
        if (status == null) {
            return Map.of("userId", userId, "status", "OFFLINE", "lastSeen", "N/A");
        }
        
        return Map.of("userId", userId, "status", status.getStatus().name(), "lastSeen", status.getLastSeen());
    }
    
    // @GetMapping("/api/chat/unread/{userId}")
    // @ResponseBody //tells to return a response body instead of a view
    // public Map<String, Long> getUnreadCount(@PathVariable String userId) {
    //     return Map.of("unreadCount", chatService.getUnreadMessageCount(userId));
    // }
}