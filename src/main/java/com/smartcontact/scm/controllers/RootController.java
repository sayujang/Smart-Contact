package com.smartcontact.scm.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.Helpers.JwtHelper;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.services.UserService;
//makes the class global to all the controllers
@ControllerAdvice
public class RootController {
    @Autowired
    UserService userService;
    @Autowired
    private JwtHelper jwtHelper;
    Logger logger=LoggerFactory.getLogger(RootController.class);
    @ModelAttribute//runs before every controller method so that model attribute is available globally
    public void addGlobalAttributes(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = Helper.getEmailOfLoggedInUser(authentication);
            User user = userService.getUserByEmail(email);
            
            if (user != null) {
                // 1. Add User (so loggedInUserId works everywhere)
                model.addAttribute("loginUser", user);

                // 2. Add Token (so userJwt works everywhere)
                String token = jwtHelper.generateToken(user.getEmail());
                model.addAttribute("jwtToken", token);
            }
        }
    }
}  
