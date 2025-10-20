package com.smartcontact.scm.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.smartcontact.scm.Helpers.Message;
import com.smartcontact.scm.Helpers.MessageType;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.forms.UserForm;
import com.smartcontact.scm.services.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class PageController {
    private final UserService userService;
    @Autowired//optional if only one constructor 
    //construtor injection
    public PageController(UserService userService)
    {
        this.userService=userService;
    }
    @RequestMapping("/home")
    public String home(Model m)
    {
        m.addAttribute("name","sayuj");
        m.addAttribute("linki", "https://github.com/");
        return "home.html";
    }
    @RequestMapping("/about")
    public String aboutPage()
    {
        return "about";
    }
    @RequestMapping("/services")
    public String servicesPage()
    {
        return "services";
    }
    @RequestMapping("/contact")
    public String contactPage()
    {
        return "contact";
    }
    @RequestMapping("/login")
    public String loginPage()
    {
        return "login";
    }
    @RequestMapping("/register")
    public String registerPage(Model m)
    {
        UserForm userForm=new UserForm();
        // userForm.setName("xyz");
        // userForm.setEmail("xyz@gmail.com");
        // userForm.setPassword("12345");
        // userForm.setPhoneNumber("xxxxxxxxxx");
        // userForm.setAbout("I am excited to ...");
        m.addAttribute("userInfo", userForm);
        return "register";
    }
    @RequestMapping(value = "/do-register", method = RequestMethod.POST)
    public String processRegister(@Valid @ModelAttribute("userInfo") UserForm userForm,BindingResult result, HttpSession session)
    {
        System.out.println(userForm);
        // User user=User.builder()
        // .name(userForm.getName())
        // .email(userForm.getEmail())
        // .password(userForm.getPassword())
        // .about(userForm.getAbout())
        // .phoneNumber(userForm.getPhoneNumber())
        // .build();
        //validate form data
        if (result.hasErrors())
        {
            return "register";
        }
        //save to database
        User user=new User();
        user.setName(userForm.getName());
        user.setEmail(userForm.getEmail());
        user.setPassword(userForm.getPassword());
        user.setAbout(userForm.getAbout());
        user.setPhoneNumber(userForm.getPhoneNumber());
        User savedUser=userService.saveUser(user);
        System.out.println("users saved");
        Message message=Message.builder().content("Registration Successful").type(MessageType.green).build();
        session.setAttribute("message", message);


        return "redirect:/register";
    }

}
