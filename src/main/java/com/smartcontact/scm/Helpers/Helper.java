package com.smartcontact.scm.Helpers;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

public class Helper {
    public static String getEmailOfLoggedInUser(Authentication authentication) {
        String username="";
        if (authentication instanceof OAuth2AuthenticationToken){
        var oauth2AuthenticationToken=(OAuth2AuthenticationToken)authentication;
        var oauthuser=(DefaultOAuth2User)authentication.getPrincipal();
        String provider=oauth2AuthenticationToken.getAuthorizedClientRegistrationId();
        if( provider.equalsIgnoreCase("google"))
        {
            username=oauthuser.getAttribute("email").toString();
        }
        else if (provider.equalsIgnoreCase("github")) 
        {
            username=oauthuser.getAttribute("email")!=null?oauthuser.getAttribute("email").toString():oauthuser.getAttribute("login").toString()+"@gmail.com";
        }
        return username;
        }
    else{
        return authentication.getName();
    }
    }
    
}