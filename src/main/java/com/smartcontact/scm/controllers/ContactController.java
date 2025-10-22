package com.smartcontact.scm.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.Helpers.Message;
import com.smartcontact.scm.Helpers.MessageType;
import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.forms.ContactForm;
import com.smartcontact.scm.services.ContactService;
import com.smartcontact.scm.services.ImageService;
import com.smartcontact.scm.services.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("user/contact")
public class ContactController {
    @Autowired
    private ContactService contactService;
    @Autowired
    private UserService userService;
    @Autowired
    private ImageService imageService;
    @RequestMapping("/add")
    public String addContactView(Model model)
    {
        ContactForm contactForm=new ContactForm();
        model.addAttribute("contactForm", contactForm);

        return "user/add_Contact";
    }
    @RequestMapping(value="/add",method = RequestMethod.POST)
    public String saveContacts(@Valid @ModelAttribute("contactForm") ContactForm contactForm,BindingResult result,Authentication authentication, HttpSession session)//make sure binding result is just after model attribute
    {
        //this method validates fields and saves contact to db and assigns logged in user as owner of contact
        //field validation:
        if (result.hasErrors())
        {
            Message message=Message.builder().content("Please correct the shown errors!").type(MessageType.red).build();
            session.setAttribute("message", message);   
            return "user/add_Contact";
        }



        //get logged in user
        String username=Helper.getEmailOfLoggedInUser(authentication);
        User user=userService.getUserByEmail(username);
        //get fileurl
        String fileUrl=imageService.uploadImage(contactForm.getContactPic()); 
        




        //convert contact form to contact entity
        Contact contact=new Contact();
        contact.setName(contactForm.getName());
        contact.setEmail(contactForm.getEmail());
        contact.setPhoneNumber(contactForm.getPhoneNumber());
        contact.setAddress(contactForm.getAddress());
        contact.setDescription(contactForm.getDescription());
        contact.setWebsiteLink(contactForm.getWebsiteLink());
        contact.setLinkedInLink(contactForm.getLinkedInLink());
        contact.setFavorite(contactForm.isFavorite());
        contact.setUser(user);
        contact.setPicture(fileUrl);


        // System.out.println(contactForm.getContactPic().getOriginalFilename());


        //save contact to db
        contactService.save(contact);
        //show success message
        Message message=Message.builder().content("Contact Added Succesfully!").type(MessageType.green).build();
        session.setAttribute("message", message);
        return "redirect:/user/contact/add";
    }
}
