package com.smartcontact.scm.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smartcontact.scm.Helpers.AppConstants;
import com.smartcontact.scm.entities.Providers;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.repositories.UserRepo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OauthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    Logger logger=LoggerFactory.getLogger(OauthAuthenticationSuccessHandler.class);
    @Autowired
    private UserRepo userRepo;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        logger.info("OAuthAuthenitcationSuccessHandler");
        //sends responds to /user/dashboard if login success
        response.sendRedirect("/user/dashboard");
        DefaultOAuth2User user=(DefaultOAuth2User)authentication.getPrincipal();
        // logger.info(user.getName());
        user.getAttributes().forEach((key,value)->{
            logger.info(key+":"+value);
        });
        logger.info(user.getAuthorities().toString());
        String email=user.getAttribute("email").toString();
        String name=user.getAttribute("name").toString();
        String picture=user.getAttribute("picture").toString();
        
        User user1=new User();
        user1.setEmail(email);
        user1.setName(name);
        user1.setProfilePic(picture);
        user1.setUserId(UUID.randomUUID().toString());
        user1.setProvider(Providers.GOOGLE);
        user1.setProviderUserId(user.getName());//gets the sub attribute which is unique id of user in google
        user1.setEmailVerified(true);
        user1.setEnabled(true);
        user1.setRoleList(List.of(AppConstants.ROLE_USER));
        User user2=userRepo.findByEmail(email).orElse(null);
        if (user2==null) 
        {
            userRepo.save(user1);
            logger.info("New user saved with email:"+email);
        }

        
}
}
