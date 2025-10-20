package com.smartcontact.scm.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/user")//only sets a basepath doesn't create endpoint
public class UserController {
    
   @GetMapping("/dashboard")
   public String userDashboard() {
       return "user/dashboard";
   }
   @GetMapping("/profile")
   public String userProfile() {
       return "user/profile";
   }
}
