package com.smartcontact.scm.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.smartcontact.scm.entities.ChatMessage.MessageStatus;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {
    
    @Id
    private String id;
    private String senderId;      // User ID who sent the message
    private String receiverId;    // User ID who receives the message
    private String content;       // Message text
    private LocalDateTime timestamp;
    private MessageStatus status=MessageStatus.SENT; 
    private MessageType type;     // TEXT, IMAGE, FILE
    
    // Constructors
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
        this.type = MessageType.TEXT;
    }
    
    public ChatMessage(String senderId, String receiverId, String content) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    // Enums
    public enum MessageStatus {
        SENT, DELIVERED, SEEN
    }
    
    public enum MessageType {
        TEXT, IMAGE, FILE
    }
}