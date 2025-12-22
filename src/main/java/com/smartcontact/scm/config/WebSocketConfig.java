package com.smartcontact.scm.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.smartcontact.scm.Helpers.JwtHelper;
import com.smartcontact.scm.services.implementation.SecurityCustomUserDetailService;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    

    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private SecurityCustomUserDetailService securityCustomUserDetailService;
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
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only check security on the initial CONNECT frame
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    String token = null;
                    if (authorization != null && !authorization.isEmpty()) {
                        String header = authorization.get(0);
                        if (header.startsWith("Bearer ")) {
                            token = header.substring(7);
                        }
                    }

                    // 1. If no token is provided, REJECT connection
                    if (token == null) {
                        System.out.println("❌ WebSocket Connection Rejected: No Token Provided");
                        return null; // <--- THIS BLOCKS THE CONNECTION
                    }

                    try {
                        String username = jwtHelper.getUsernameFromToken(token);
                        UserDetails userDetails = securityCustomUserDetailService.loadUserByUsername(username);
                        
                        // 2. If token is valid, AUTHENTICATE the user
                        if (jwtHelper.validateToken(token, userDetails.getUsername())) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            accessor.setUser(auth);
                            System.out.println("✅ WebSocket Authenticated User: " + username);
                        } else {
                            // 3. If validation fails, REJECT
                            System.out.println("❌ WebSocket Connection Rejected: Invalid Token Signature");
                            return null; // <--- THIS BLOCKS THE CONNECTION
                        }
                    } catch (Exception e) {
                         // 4. If any error occurs (expired, malformed), REJECT
                         System.out.println("❌ WebSocket Connection Rejected: Token Error -> " + e.getMessage());
                         return null; // <--- THIS BLOCKS THE CONNECTION
                    }
                }

                return message;
            }
        });
    }
}