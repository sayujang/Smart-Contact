package com.smartcontact.scm.repositories;

import com.smartcontact.scm.entities.ChatMessage;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    // FIX: Using @Query ensures we check both directions accurately
    // Logic: (Sender = A AND Receiver = B) OR (Sender = B AND Receiver = A)
    @Query(value = "{ '$or': [ " +
           "{ 'senderId': ?0, 'receiverId': ?1 }, " +
           "{ 'senderId': ?1, 'receiverId': ?0 } " +
           "] }", sort = "{ 'timestamp' : 1 }")
    List<ChatMessage> findByChatHistory(String userId1, String userId2);
    @Aggregation(pipeline = {
        "{ '$match': { 'receiverId': ?0 } }",
        "{ '$group': { '_id': '$senderId' } }"
    })
    List<String> findDistinctSendersByReceiverId(String receiverId);
    
    // Keep these for your unread counts
    long countByReceiverIdAndStatus(String receiverId, ChatMessage.MessageStatus status);
    List<ChatMessage> findByReceiverIdAndStatus(String userId,ChatMessage.MessageStatus status);
    
}