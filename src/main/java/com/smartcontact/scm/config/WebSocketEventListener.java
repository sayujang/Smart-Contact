package com.smartcontact.scm.config;

import com.smartcontact.scm.entities.UserStatus;
import com.smartcontact.scm.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.Map;

@Component
public class WebSocketEventListener {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // We handle "Online" status in the Controller (/chat.connect)
        // So we don't need to do anything here.
        System.out.println("New Connection Detected");
    }
    //when websocket disconnects it sends sessionDisconnectEvent which is handled by this function
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        //retrieve the userid that was just saved in the chat controllers addUser method 
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        
        if (userId != null) {
            System.out.println("User Disconnected: " + userId);
            
            //updates the database for specific userid to be offline
            chatService.updateUserStatus(userId, UserStatus.Status.OFFLINE, null);
            
            //broadcast json payload to frontend
            messagingTemplate.convertAndSend(
                "/topic/user.status",
                Map.of("userId", userId, "status", "OFFLINE")
            );
        }
    }
}