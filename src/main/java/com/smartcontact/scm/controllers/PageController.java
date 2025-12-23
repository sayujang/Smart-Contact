package com.smartcontact.scm.controllers;

import java.util.List;
import java.util.UUID;

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
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.forms.UserForm;
import com.smartcontact.scm.services.ChatService;
import com.smartcontact.scm.services.EmailService;
import com.smartcontact.scm.services.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class PageController {
    private final UserService userService;
    @Autowired//optional if only one constructor 
    //construtor injection
    public PageController(UserService userService)
    {
        this.userService=userService;
    }
    @Autowired
    private ChatService chatService;
    @Autowired
    private EmailService emailService;
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
    @RequestMapping("/home")
    public String home(Model m)
    {
        m.addAttribute("name","sayuj");
        m.addAttribute("linki", "https://github.com/");
        return "home";
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
            Message message=Message.builder().content("Please correct the shown errors!").type(MessageType.red).build();
            session.setAttribute("message", message);  
            return "register";
        }
        if (userService.isUserExistByEmail(userForm.getEmail())){
            session.setAttribute("message", Message.builder().content("Email already registered! Try logging in.").type(MessageType.red).build());
            return "register";
        }
        
        //save to database
        User user=new User();
        user.setName(userForm.getName());
        user.setEmail(userForm.getEmail());
        user.setPassword(userForm.getPassword());
        user.setAbout(userForm.getAbout());
        user.setPhoneNumber(userForm.getPhoneNumber());


        user.setEnabled(false); 
        user.setEmailVerified(false);

        String token = UUID.randomUUID().toString();
        user.setEmailToken(token);

        User savedUser=userService.saveUser(user);
        System.out.println("users saved");
        String link = "http://localhost:8080/auth/verify-email?token=" + token; 
        String body = "Hello " + user.getName() + ",\n\n"
                    + "Please click the link below to verify your account:\n" + link;
        emailService.sendEmail(user.getEmail(), "SCM Account Verification", body);
        System.out.println("Verification mail sent");
        Message message=Message.builder().content("Registration Successful! Check you email to verify.").type(MessageType.green).build();
        session.setAttribute("message", message);


        return "redirect:/register";
    }
    @GetMapping("/auth/verify-email")
    public String verifyEmail(@RequestParam("token") String token, HttpSession session) {
        
        User user = userService.getUserByToken(token);

        if (user != null) {
            if (user.isEmailVerified()) {
                session.setAttribute("message", Message.builder()
                    .content("Email is already verified. Login now.")
                    .type(MessageType.blue).build());
                return "redirect:/login";
            }

            // Verify the user
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setEmailToken(null); // Clear token (security best practice)
            
            userService.updateUser(user);

            session.setAttribute("message", Message.builder()
                .content("Email Verified Successfully! You can now login.")
                .type(MessageType.green).build());
            
            return "redirect:/login";
            
        } else {
            session.setAttribute("message", Message.builder()
                .content("Invalid or Expired Verification Link.")
                .type(MessageType.red).build());
            return "redirect:/register";
        }
    }
    @GetMapping("/user/chat/requests")
    public String viewMessageRequests(Model model, Authentication authentication) {
        // get logged in user
        String username = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(username);

        // get unknown users
        List<User> unknownUsers = chatService.getUnknownUsers(user.getUserId());

        //add to model 
        model.addAttribute("unknownUsers", unknownUsers);
        //return the view
        return "user/message_requests";
    }

}



// Browser
//   └── JSESSIONID cookie
//         ↓
// Server (RAM)
//   └── HttpSession
//         ├── SPRING_SECURITY_CONTEXT (auth)
//            └── Authentication
//         └── "message" (UI flash)


// ┌──────────┐
// │ Browser  │
// └────┬─────┘
//      │
//      │ 1️⃣ POST /login (username + password)
//      ▼
// ┌──────────┐
// │ Server   │
// │ (Spring  │
// │ Security)(automatic)│
// └────┬─────┘
//      │
//      │ 2️⃣ Authenticate user
//      │
//      │ 3️⃣ Create HttpSession (stored in RAM)
//      │    ┌─────────────────────────────┐
//      │    │ HttpSession                  │
//      │    │ ├─ SPRING_SECURITY_CONTEXT   │
//      │    │ │   └─ Authentication        │
//      │    │ │      ├─ UserDetails        │
//      │    │ │      ├─ Roles              │
//      │    │ │      └─ authenticated=true │
//      │    │ └─ "message" (UI flash)      │
//      │    └─────────────────────────────┘
//      │
//      │ 4️⃣ Generate Session ID
//      │    JSESSIONID = A1B2C3D4
//      │
//      │ 5️⃣ Send HTTP Response
//      │    Set-Cookie: JSESSIONID=A1B2C3D4; HttpOnly
//      ▼
// ┌──────────┐
// │ Browser does the job of creating cookies and sending automatically  │
// └────┬─────┘
//      │
//      │ 6️⃣ Store cookie internally
//      │
//      │ 7️⃣ Next request (any page / API)
//      │    Cookie: JSESSIONID=A1B2C3D4
//      ▼
// ┌──────────┐
// │ Server   │
// │ (Spring) │
// └────┬─────┘
//      │
//      │ 8️⃣ Read cookie
//      │
//      │ 9️⃣ Lookup session in RAM
//      │
//      │ 🔟 Restore SecurityContext
//      │
//      │ 1️⃣1️⃣ Inject Authentication
//      │        into Controller
//      ▼
// ┌───────────────────────────────┐
// │ @Controller method executes   │
// │ Authentication authentication │
// │ is AVAILABLE                  │
// └───────────────────────────────┘


// ┌──────────┐
// │ Browser / Client │
// └────┬─────┘
//      │
//      │ 1️⃣ User logs in (POST /login with username/password)
//      ▼
// ┌──────────┐
// │ Server   │
// │ (AuthController)(manual work) │
// └────┬─────┘
//      │
//      │ 2️⃣ Authenticate user
//      │
//      │ 3️⃣ Generate JWT (signed token)
//      │    {
//      │      sub: "john",
//      │      role: "USER",
//      │      exp: 1712345678
//      │    }
//      │
//      │ 4️⃣ Send JWT in response body
//      │    { "token": "<JWT_STRING>" }
//      ▼
// ┌──────────┐
// │ Browser / Client storage and header addition is manual │
// └────┬─────┘
//      │
//      │ 5️⃣ Store JWT in client
//      │    - localStorage
//      │    - sessionStorage
//      │    - memory (not HttpOnly)
//      │
//      │ 6️⃣ For every request / WebSocket connection
//      │    Add header:
//      │    Authorization: Bearer <JWT_STRING>
//      ▼
// ┌──────────┐
// │ Server   │
// │ (Filter / Interceptor) │
// └────┬─────┘
//      │
//      │ 7️⃣ Extract JWT from Authorization header
//      │
//      │ 8️⃣ Validate signature & expiration
//      │
//      │ 9️⃣ Parse token → Authentication object
//      │
//      │ 🔟 Set SecurityContext / attach to request
//      ▼
// ┌───────────────────────────────┐
// │ Controller / WebSocket handler│
// │ Authentication is AVAILABLE   │
// └───────────────────────────────┘
