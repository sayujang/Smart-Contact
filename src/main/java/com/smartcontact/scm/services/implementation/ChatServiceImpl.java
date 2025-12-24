package com.smartcontact.scm.services.implementation;

import com.smartcontact.scm.entities.ChatMessage;
import com.smartcontact.scm.entities.ChatMessage.MessageStatus;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.entities.UserStatus;
import com.smartcontact.scm.repositories.ChatMessageRepository;
import com.smartcontact.scm.repositories.ContactRepo;
import com.smartcontact.scm.repositories.UserRepo;
import com.smartcontact.scm.repositories.UserStatusRepository;
import com.smartcontact.scm.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private UserStatusRepository userStatusRepository;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ContactRepo contactRepo;
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
@Override
    public List<User> getUnknownUsers(String currentUserId) {
        // 1. Get IDs of everyone who has messaged me
        List<String> senderIds = chatMessageRepository.findDistinctSendersByReceiverId(currentUserId);
        
        // 2. Get Emails of everyone I have saved as a contact
        List<String> myContactEmails = contactRepo.findEmailsByUserId(currentUserId);
        
        // 3. Fetch User objects for the sender IDs
        List<User> senders = userRepo.findAllById(senderIds);

        // 4. Filter: Keep sender ONLY IF their email is NOT in my contact list
        return senders.stream()
                .filter(sender -> !myContactEmails.contains(sender.getEmail()))
                .filter(sender -> !sender.getUserId().equals(currentUserId)) // Ensure I don't see myself
                .collect(Collectors.toList());
    }

@Override
public boolean markMessagesAsSeen(String senderId, String receiverId) { // boolean return type

    User receiver = userRepo.findById(receiverId).orElse(null); // The one reading
    User sender = userRepo.findById(senderId).orElse(null);     // The one who sent

    if (receiver == null || sender == null) return false;

    // CHECK 1: Does the RECEIVER have the SENDER saved?
    // (If I don't know you, I don't want you to know I read this)
    boolean receiverHasSavedSender = contactRepo.findByUserAndEmail(receiver, sender.getEmail()).isPresent();

    // CHECK 2: Does the SENDER have the RECEIVER saved?
    // (If you don't know me, I shouldn't be sending you status updates)
    boolean senderHasSavedReceiver = contactRepo.findByUserAndEmail(sender, receiver.getEmail()).isPresent();

    // LOGIC: BOTH must be true for Blue Ticks to appear
    if (!receiverHasSavedSender || !senderHasSavedReceiver) {
        System.out.println("Privacy: Blocked Read Receipt. Mutual contact required.");
        return false; // Return false so Controller doesn't send the update
    }

    // If both checks pass, update the Database
    Query query = new Query(
        Criteria.where("senderId").is(senderId)
                .and("receiverId").is(receiverId)
                .and("status").ne(MessageStatus.SEEN)
    );

    Update update = new Update();
    update.set("status", MessageStatus.SEEN);

    mongoTemplate.updateMulti(query, update, ChatMessage.class);
    
    return true; // Success
}
@Override
public Map<String, Long> getUnreadCountsSplit(String userId) {
    // 1. Get all unread messages for this user
    List<ChatMessage> unreadMessages = chatMessageRepository.findByReceiverIdAndStatus(userId, ChatMessage.MessageStatus.SENT);
    
    // 2. Get my saved contacts' emails
    List<String> myContactEmails = contactRepo.findEmailsByUserId(userId);
    
    // 3. Get User details for the senders (to match IDs to Emails)
    // (We need to know the email of the person who sent the message to check against contact list)
    List<String> senderIds = unreadMessages.stream().map(ChatMessage::getSenderId).distinct().collect(Collectors.toList());
    List<User> senders = userRepo.findAllById(senderIds);
    Map<String, String> senderIdToEmailMap = senders.stream()
            .collect(Collectors.toMap(User::getUserId, User::getEmail));

    long contactsCount = 0;
    long requestsCount = 0;

    for (ChatMessage msg : unreadMessages) {
        String senderEmail = senderIdToEmailMap.get(msg.getSenderId());
        
        // If sender's email is in my contacts -> It's a Contact Message
        if (senderEmail != null && myContactEmails.contains(senderEmail)) {
            contactsCount++;
        } 
        // If not -> It's a Message Request
        else {
            requestsCount++;
        }
    }

    return Map.of("contactsUnread", contactsCount, "requestsUnread", requestsCount);
}
@Override
public List<String> getSendersWithUnreadMessages(String receiverId) {
    // Determine senders who have messages with status 'SENT' addressed to this receiver
    Query query = new Query();
    query.addCriteria(Criteria.where("receiverId").is(receiverId)
                      .and("status").is(ChatMessage.MessageStatus.SENT));
    
    // distinct query to get unique sender IDs
    return mongoTemplate.findDistinct(query, "senderId", ChatMessage.class, String.class);
}
}