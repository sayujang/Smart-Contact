package com.smartcontact.scm.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.services.UserService;
//makes the class global to all the controllers
@ControllerAdvice
public class RootController {
    @Autowired
    UserService userService;
    Logger logger=LoggerFactory.getLogger(RootController.class);
    @ModelAttribute//runs before every controller method so that model attribute is available globally
    public void addLoggedInUserInfo(Model m,Authentication authentication) {
        if(authentication==null)
        {
             m.addAttribute("loginUser", null);
            return;
        }
       String username=Helper.getEmailOfLoggedInUser(authentication);//this is an email
        System.out.println("Logged in user: "+username);
        User user=userService.getUserByEmail(username);
        logger.info(user.getName());
        m.addAttribute("loginUser", user);
    }
}  
