package com.smartcontact.scm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smartcontact.scm.services.implementation.SecurityCustomUserDetailService;

@Configuration
public class SecurityConfig {
    
    @Autowired
    private SecurityCustomUserDetailService userDetailService;
    
    @Autowired
    private OauthAuthenticationSuccessHandler oauthAuthenticationSuccessHandler;
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(authorize -> {
            // WebSocket endpoints - must be accessible
            authorize.requestMatchers("/ws/**").permitAll();
            //it misses stomp message security as these are not real http endpoints
            authorize.requestMatchers("/app/**").permitAll();
            authorize.requestMatchers("/topic/**").permitAll();
            authorize.requestMatchers("/queue/**").permitAll();
            
            // Chat API endpoints - authenticated users only
            authorize.requestMatchers("/api/chat/**").authenticated();
            authorize.requestMatchers("/api/contact/is-user/**").authenticated();
            
            // User pages - authenticated
            authorize.requestMatchers("/user/**").authenticated();
            authorize.requestMatchers("/auth/**", "/do-register", "/register").permitAll();
            // Everything else - permit all
            authorize.anyRequest().permitAll();
        });
        
        httpSecurity.formLogin(form -> {
            form.loginPage("/login");
            form.loginProcessingUrl("/authenticate");
            form.permitAll();
            form.successForwardUrl("/user/dashboard");
            form.failureForwardUrl("/login?error=true");
            form.usernameParameter("email");
            form.passwordParameter("password");
        });
        
        // CSRF disabled - good for WebSocket
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        
        httpSecurity.logout(logout -> {
            logout.logoutUrl("/do-logout");
            logout.logoutSuccessUrl("/login?logout=true");
        });
        
        httpSecurity.oauth2Login(login -> {
            login.loginPage("/login");
            login.successHandler(oauthAuthenticationSuccessHandler);
        });
        
        return httpSecurity.build();
    }
}