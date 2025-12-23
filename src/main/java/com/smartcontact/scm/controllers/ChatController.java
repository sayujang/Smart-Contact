package com.smartcontact.scm.controllers;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.Helpers.ResourceNotFoundException;
import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.entities.UserStatus;
import com.smartcontact.scm.repositories.ChatMessageRepository;
import com.smartcontact.scm.repositories.ContactRepo;
import com.smartcontact.scm.services.ChatService;
import com.smartcontact.scm.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private UserService userService;

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    //sending messages
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        // save to Database
        ChatMessage savedMessage = chatService.saveMessage(message);

        // send to receiver
        messagingTemplate.convertAndSend(
            "/queue/messages/" + message.getReceiverId(), 
            savedMessage
        );

        // send to sender (so it shows up on their screen immediately)
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
    public void addUser(@Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        
        //make the user online and add a dummy session
        chatService.updateUserStatus(userId, UserStatus.Status.ONLINE, "websocket-session");

        //get user by id
        User me = userService.getUserById(userId).orElseThrow(()-> new ResourceNotFoundException("user not found"));
        
        //return list of contacts which has my email
        List<Contact> peopleWhoHaveAddedMe = contactRepo.findByEmail(me.getEmail());

        for (Contact contactEntry : peopleWhoHaveAddedMe) {
            //gets the user that my contact belongs to 
            User friend = contactEntry.getUser(); 
            
           //check if i also have them as a contact
            boolean isMutual = contactRepo.existsByUserAndEmail(me, friend.getEmail());

            if (isMutual) {
                //send online update only to this specific friend's private queue
                Map<String, String> update = Map.of("userId", userId, "status", "ONLINE");
                messagingTemplate.convertAndSend("/queue/status/" + friend.getUserId(), update);
            }
        }
    }

   @MessageMapping("/chat.disconnect")
    public void disconnectUser(@Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        
        //first update db
        chatService.updateUserStatus(userId, UserStatus.Status.OFFLINE, null);

        //get my userid and the contacts which has my email
        User me = userService.getUserById(userId).orElseThrow(()-> new ResourceNotFoundException("no user found"));
        List<Contact> peopleWhoHaveAddedMe = contactRepo.findByEmail(me.getEmail());

        for (Contact contactEntry : peopleWhoHaveAddedMe) {
            User friend = contactEntry.getUser(); 
            boolean isMutual = contactRepo.existsByUserAndEmail(me, friend.getEmail());

            if (isMutual) {
               //send offline message to private queue of my friend
                Map<String, String> update = Map.of("userId", userId, "status", "OFFLINE");
                messagingTemplate.convertAndSend("/queue/status/" + friend.getUserId(), update);
            }
        }
    }


    //API ENDPOINTS (For loading history via HTTP)
    @GetMapping("/api/chat/history/{userId1}/{userId2}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String userId1, @PathVariable String userId2) {
        return chatService.getConversationHistory(userId1, userId2);
    }

    @GetMapping("/api/chat/status/{userId}") // Ensure path matches your JS
    @ResponseBody
    public Map<String, Object> getUserStatus(@PathVariable String userId, Authentication authentication) {
        String myEmail = Helper.getEmailOfLoggedInUser(authentication);
        User me = userService.getUserByEmail(myEmail); // Me
        User them = userService.getUserById(userId).orElseThrow(()-> new ResourceNotFoundException("user not found"));   // The person I'm checking

        // Mutual Check
        boolean iHaveThem = contactRepo.existsByUserAndEmail(me, them.getEmail());
        boolean theyHaveMe = contactRepo.existsByUserAndEmail(them, myEmail);

        boolean isOnline = false;

        // ONLY check real status if we are mutual friends
        if (iHaveThem && theyHaveMe) {
            // Use your service's method which reads from UserStatusRepository
            isOnline = chatService.isUserOnline(userId);
        }
        // If not mutual, 'isOnline' remains false (OFFLINE)

        return Map.of(
            "userId", userId, 
            "status", isOnline ? "ONLINE" : "OFFLINE"
        );
    }
    
    @GetMapping("/api/chat/unread/{userId}")
    @ResponseBody //tells to return a response body instead of a view
    public Map<String, Long> getUnreadCount(@PathVariable String userId) {
        return Map.of("unreadCount", chatService.getUnreadMessageCount(userId));
    }
    @GetMapping("/api/chat/unknown/{userId}")
    @ResponseBody
    public List<User> getUnknownUsers(@PathVariable String userId) {
        // This returns a list of Users who messaged 'userId' but are NOT in 'userId's contacts
        return chatService.getUnknownUsers(userId);
    }

    @MessageMapping("/chat.read")
public void markMessagesAsRead(@Payload Map<String, String> payload) {
    String senderId = payload.get("senderId");   
    String receiverId = payload.get("receiverId"); 

    // 1. Try to Update Database
    // We capture the result (True = Updated, False = Blocked by Privacy)
    boolean isUpdated = chatService.markMessagesAsSeen(senderId, receiverId);

    // 2. ONLY notify the Sender if the database was actually updated
    if (isUpdated) {
        messagingTemplate.convertAndSend(
            "/queue/read/" + senderId, 
            Map.of("receiverId", receiverId, "status", "SEEN")
        );
    }
}
}