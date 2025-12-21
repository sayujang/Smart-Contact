package com.smartcontact.scm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); //topic is a broadcast destination and queue is private(one to one) destination //they are used to deliver to anyone subscribe to themby a broker
        config.setApplicationDestinationPrefixes("/app"); // to specify if a client sends at /app/chat.sendMessage then first the request goes to controller in java having @MessageMapping("/chat.sendMessage")
        config.setUserDestinationPrefix("/user"); //used to uniquely set the destination for each user
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") //move from standard http  connection to websocket connection
                .setAllowedOriginPatterns("*")
                .withSockJS(); //incase web browser doesnt support websockets
    }
}