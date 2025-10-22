package com.smartcontact.scm.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
        var OAuth2AuthenticationToken=(OAuth2AuthenticationToken)authentication;
        var oauthuser=(DefaultOAuth2User)authentication.getPrincipal();
        String provider=OAuth2AuthenticationToken.getAuthorizedClientRegistrationId();//google or github
        //checking default values from oauth2 login
        User user=new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setRoleList(List.of(AppConstants.ROLE_USER));
        user.setEmailVerified(true);
        user.setEnabled(true);
        System.out.println(oauthuser.getAttributes());
        if(provider.equalsIgnoreCase("google"))
        {
            user.setEmail(oauthuser.getAttribute("email").toString());
            user.setProvider(Providers.GOOGLE);
            user.setProviderUserId(user.getName());
            user.setProfilePic(oauthuser.getAttribute("picture").toString());
            user.setName(oauthuser.getAttribute("name").toString());
        }
        else if (provider.equalsIgnoreCase("github")) 
        {
            user.setEmail(oauthuser.getAttribute("email")!=null?oauthuser.getAttribute("email").toString():oauthuser.getAttribute("login").toString()+"@gmail.com");
            user.setProvider(Providers.GITHUB);
            user.setName(oauthuser.getAttribute("login").toString());
            user.setProfilePic(oauthuser.getAttribute("avatar_url").toString());
            user.setProviderUserId(oauthuser.getName());
        }
        String email=user.getEmail();
        User user1=userRepo.findByEmail(email).orElse(null);
        if (user1==null) 
        {
            userRepo.save(user);
            logger.info("New user saved with email:"+email);
        }
        //sends responds to /user/dashboard if login success
        response.sendRedirect("/user/dashboard");
        
}
}
