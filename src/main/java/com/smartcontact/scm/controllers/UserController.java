package com.smartcontact.scm.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.services.UserService;


@Controller
@RequestMapping("/user")//only sets a basepath doesn't create endpoint
public class UserController {
    Logger logger=LoggerFactory.getLogger(UserController.class);
    @Autowired
    UserService userService;
    
   @GetMapping("/dashboard")
   public String userDashboard() {
       return "user/dashboard";
   }
   @RequestMapping("/profile")
   public String userProfile(Model m, Authentication authentication) {
       return "user/profile";
   }
}
