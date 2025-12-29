package com.smartcontact.scm.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.Helpers.Message;
import com.smartcontact.scm.Helpers.MessageType;
import com.smartcontact.scm.entities.Providers;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.services.ContactService;
import com.smartcontact.scm.services.ImageService;
import com.smartcontact.scm.services.UserService;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/user")//only sets a basepath doesn't create endpoint
public class UserController {
    Logger logger=LoggerFactory.getLogger(UserController.class);
    @Autowired
    UserService userService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ContactService contactService;
    @Autowired
    private PasswordEncoder passwordEncoder;
   @GetMapping("/settings")
    public String openSettings(Model model, Authentication authentication) {
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);
        model.addAttribute("user", user);
        return "user/settings";
    }

    //handle profile update
    @PostMapping("/settings/update")
public String updateProfile(
        @ModelAttribute User userForm,
        @RequestParam("profileImage") MultipartFile file,
        Authentication authentication,
        HttpSession session) {

    String username = Helper.getEmailOfLoggedInUser(authentication);
    User oldUser = userService.getUserByEmail(username);

    try {
        //update user object
        oldUser.setName(userForm.getName());
        oldUser.setPhoneNumber(userForm.getPhoneNumber());
        oldUser.setAbout(userForm.getAbout());

        if (!file.isEmpty()) {
            String fileName = UUID.randomUUID().toString();
            String imageUrl = imageService.uploadImage(file, fileName);
            oldUser.setProfilePic(imageUrl);
        }

        //save to db
        userService.updateUser(oldUser);

        //sycnc changes to contact
        // This will find everyone else's contact list that has this email 
        // and update the name, phone, and pic.
        contactService.syncUserChangesToContacts(oldUser);

        session.setAttribute("message", Message.builder()
            .content("Profile updated!")
            .type(MessageType.green).build());

    } catch (Exception e) {
        e.printStackTrace();
        session.setAttribute("message", Message.builder()
            .content("Something went wrong: " + e.getMessage())
            .type(MessageType.red).build());
    }

    return "redirect:/user/settings";
}

    //handle password change
    @PostMapping("/settings/change-password")
public String changePassword(
        @RequestParam(value = "oldPassword", required = false) String oldPassword,
        @RequestParam("newPassword") String newPassword,
        Authentication authentication,
        HttpSession session) {

    String username = Helper.getEmailOfLoggedInUser(authentication);
    User user = userService.getUserByEmail(username);

    //for oauth2 login no password to verify
    if (user.getProvider() != Providers.SELF) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);
        
        session.setAttribute("message", Message.builder()
            .content("Password set successfully! You can now login with email/password too.")
            .type(MessageType.green).build());
            
        return "redirect:/user/settings";
    }

    //for self registered, old password must be verified
    if (user.getProvider() == Providers.SELF) {
        if (oldPassword == null || oldPassword.isEmpty()) {
           session.setAttribute("message", Message.builder()
                .content("Add the missing old password!")
                .type(MessageType.red).build());
            return "redirect:/user/settings";
        }
        //passwordEncdoer rehashes the old password from form login to compare with stored hashed password
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            session.setAttribute("message", Message.builder()
                .content("Password changed successfully!")
                .type(MessageType.green).build());
        } else {
            session.setAttribute("message", Message.builder()
                .content("Invalid old password!")
                .type(MessageType.red).build());
        }
    }

    return "redirect:/user/settings";
}

    //handles account deletion
    @PostMapping("/settings/delete") 
    public String deleteAccount(Authentication authentication, HttpSession session) {
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);

        // This deletes the user AND automatically cascades to delete all contacts
        userService.deleteUser(user.getUserId());

        // Logout logic should happen here or via Spring Security
        session.setAttribute("message", Message.builder()
                .content("User deleted successfully!")
                .type(MessageType.blue).build());
        // session.invalidate(); 
        
        return "redirect:/register"; // Redirects to login page
    }
}
