package com.smartcontact.scm.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {
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
    public String registerPage()
    {
        return "register";
    }

}
