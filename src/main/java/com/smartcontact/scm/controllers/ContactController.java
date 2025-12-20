package com.smartcontact.scm.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartcontact.scm.Helpers.AppConstants;
import com.smartcontact.scm.Helpers.Helper;
import com.smartcontact.scm.Helpers.Message;
import com.smartcontact.scm.Helpers.MessageType;
import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.forms.ContactForm;
import com.smartcontact.scm.forms.ContactSearchForm;
import com.smartcontact.scm.services.ContactService;
import com.smartcontact.scm.services.ImageService;
import com.smartcontact.scm.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/add")
    public String addContactView(Model model) {
        ContactForm contactForm = new ContactForm();
        model.addAttribute("contactForm", contactForm);

        return "user/add_Contact";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String saveContacts(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult result,
            Authentication authentication, HttpSession session)// make sure binding result is just after model attribute
    {
        // this method validates fields and saves contact to db and assigns logged in
        // user as owner of contact
        // field validation:
        if (result.hasErrors()) {
            Message message = Message.builder().content("Please correct the shown errors!").type(MessageType.red)
                    .build();
            session.setAttribute("message", message);
            return "user/add_Contact";
        }

        // get logged in user
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);
        // get fileurl

        // convert contact form to contact entity
        Contact contact = new Contact();
        contact.setName(contactForm.getName());
        contact.setEmail(contactForm.getEmail());
        contact.setPhoneNumber(contactForm.getPhoneNumber());
        contact.setAddress(contactForm.getAddress());
        contact.setDescription(contactForm.getDescription());
        contact.setWebsiteLink(contactForm.getWebsiteLink());
        contact.setLinkedInLink(contactForm.getLinkedInLink());
        contact.setFavorite(contactForm.isFavorite());
        contact.setUser(user);

        if (contactForm.getContactPic() != null && !contactForm.getContactPic().isEmpty()) {
            String filename = UUID.randomUUID().toString();// gennerate random filename for each image
            String fileUrl = imageService.uploadImage(contactForm.getContactPic(), filename);
            contact.setPicture(fileUrl);
            contact.setCloudinaryPublicId(filename);// set the contacs pic public id
        }

        // System.out.println(contactForm.getContactPic().getOriginalFilename());

        // save contact to db
        contactService.save(contact);
        // show success message
        Message message = Message.builder().content("Contact Added Succesfully!").type(MessageType.green).build();
        session.setAttribute("message", message);
        return "redirect:/user/contact/add";
    }

    // uses the endpoint defined in class level request mapping
    @RequestMapping // get request by default//get methods are essential for pagination  not requestparam or model attribute
    public String viewContacts(
        @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = AppConstants.PAGE_SIZE + "") int size,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction, Model model,
            Authentication authentication) {
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);
        Page<Contact> pageContact = contactService.getByUser(user, page, size, sortBy, direction);
        model.addAttribute("pageContact", pageContact);
        model.addAttribute("contactSearchForm", new ContactSearchForm());
        model.addAttribute("pageSize", AppConstants.PAGE_SIZE);
        return "user/view_contacts";
    }

    // search handler
    @RequestMapping("/search")
    public String searchHandler(@ModelAttribute("contactSearchForm") ContactSearchForm contactSearchForm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = AppConstants.PAGE_SIZE + "") int size,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction,
            @RequestParam("searchType") String searchType,
            @RequestParam("query") String query,
            Model model, Authentication authentication) {
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);
        Page<Contact> pageContact = null;
        if (contactSearchForm.getSearchType().equalsIgnoreCase("name")) {
            pageContact = contactService.searchByName(contactSearchForm.getQuery(), page, size, sortBy, sortBy, user);
        } else if (contactSearchForm.getSearchType().equalsIgnoreCase("email")) {
            pageContact = contactService.searchByEmail(contactSearchForm.getQuery(), page, size, sortBy, sortBy, user);
        } else if (contactSearchForm.getSearchType().equalsIgnoreCase("phone")) {
            pageContact = contactService.searchByPhoneNumber(contactSearchForm.getQuery(), page, size, sortBy, sortBy, user);
        }
        logger.info("pagecontact {}", pageContact.getContent());
        model.addAttribute("pageContact", pageContact);
        model.addAttribute("pageSize", AppConstants.PAGE_SIZE);
        model.addAttribute("contactSearchForm", contactSearchForm);
        logger.info("Search Type: " + searchType);
        logger.info("Query: " + query);
        return "user/search";
    }
    @RequestMapping("/delete/{contactId}")
    public String deleteContact(@PathVariable String contactId, HttpSession session)
    {
        contactService.delete(contactId);
         Message message=Message.builder().content("Contact Deleted Successfully!").type(MessageType.green).build();
            session.setAttribute("message", message);  
        return "redirect:/user/contact";
    }
    @RequestMapping("/update_view/{contactId}")
    public String updateViewContact(@PathVariable String contactId,Model model)
    {
        Contact contact=contactService.getById(contactId);
        ContactForm contactForm=new ContactForm();
        contactForm.setName(contact.getName());
        contactForm.setEmail(contact.getEmail());
        contactForm.setAddress(contact.getAddress());
        contactForm.setDescription(contact.getDescription());
        contactForm.setPhoneNumber(contact.getPhoneNumber());
        contactForm.setLinkedInLink(contact.getLinkedInLink());
        contactForm.setWebsiteLink(contact.getWebsiteLink());
        contactForm.setFavorite(contact.isFavorite());
        contactForm.setPictureUrl(contact.getPicture());
        model.addAttribute("contactForm", contactForm);
        model.addAttribute("contactId",contactId );
        return "user/update_view";
    }
    @RequestMapping("/update/{contactId}")
    public String updateContact(@PathVariable String contactId,@Valid @ModelAttribute ContactForm contactForm,BindingResult bindingResult,Model model,HttpSession session)
    {

        if(bindingResult.hasErrors())
        {
            Message message = Message.builder().content("Please correct the shown errors!").type(MessageType.red)
                    .build();
            session.setAttribute("message", message);
            return "user/update_view";
        }
        var con = contactService.getById(contactId);
        con.setId(contactId);
        con.setName(contactForm.getName());
        con.setEmail(contactForm.getEmail());
        con.setPhoneNumber(contactForm.getPhoneNumber());
        con.setAddress(contactForm.getAddress());
        con.setDescription(contactForm.getDescription());
        con.setFavorite(contactForm.isFavorite());
        con.setWebsiteLink(contactForm.getWebsiteLink());
        con.setLinkedInLink(contactForm.getLinkedInLink());

        if (contactForm.getContactPic() != null && !contactForm.getContactPic().isEmpty()) {
            logger.info("file is not empty");
            String fileName = UUID.randomUUID().toString();
            String imageUrl = imageService.uploadImage(contactForm.getContactPic(), fileName);
            con.setCloudinaryPublicId(fileName);
            con.setPicture(imageUrl);
            contactForm.setPictureUrl(imageUrl);

        } else {
            logger.info("file is empty");
        }
        var updateCon = contactService.update(con);
        Message message = Message.builder().content("Contact Updated Succesfully!").type(MessageType.green).build();
        session.setAttribute("message", message);
        return "redirect:/user/contact/update_view/"+contactId;
    }
}
