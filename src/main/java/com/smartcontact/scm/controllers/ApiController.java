package com.smartcontact.scm.controllers;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.services.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.services.ContactService;

@RestController
@RequestMapping("/api")
public class ApiController {
    
    @Autowired
    private ContactService contactService;
    @Autowired
    private UserService userService;

    //this is for viewing contact info in the contact_modal
    @GetMapping("contact/{contactId}")
    public Contact getContact(@PathVariable String contactId)
    {
        return contactService.getById(contactId);
    }

    //checks whether the email is a registered user and then returns all user info including userId
    @GetMapping("/contact/is-user/{email}")
public Map<String, Object> isContactAUser(@PathVariable String email) {
    Map<String, Object> response = new HashMap<>();
    
    System.out.println("=== API: Checking if contact is a user ===");
    System.out.println("Email: " + email);
    
    try {
        // Find user by email using UserService
        User user = userService.getUserByEmail(email);
        
        if (user != null) {
            System.out.println("✓ User found: " + user.getName() + " (ID: " + user.getUserId() + ")");
            
            response.put("isUser", true);
            response.put("userId", user.getUserId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("profilePic", user.getProfilePic() != null ? user.getProfilePic() : "/images/user.png");
            
            return response;
        } else {
            System.out.println("✗ User object is null for email: " + email);
        }
        
    } catch (Exception e) {
        System.out.println("✗ Exception: " + e.getMessage());
        e.printStackTrace();
    }
    
    // User not found
    System.out.println("✗ Final result: User NOT registered");
    response.put("isUser", false);
    response.put("message", "Contact is not a registered user");
    
    return response;
}
     
}
