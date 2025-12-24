package com.smartcontact.scm.services.implementation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartcontact.scm.Helpers.AppConstants;
import com.smartcontact.scm.Helpers.ResourceNotFoundException;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.repositories.ContactRepo;
import com.smartcontact.scm.repositories.UserRepo;
import com.smartcontact.scm.services.UserService;

@Service
public class userServiceImpl implements UserService {
    //define password encoder
    private final PasswordEncoder passwordEncoder;
    private Logger logger=LoggerFactory.getLogger(this.getClass());

    @Autowired
    private  UserRepo userRepo;//field injection
    
    @Autowired
    private ContactRepo contactRepo;
    //constructor injection
    userServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public User saveUser(User user) {
        // Create random userId
        String userId = UUID.randomUUID().toString();
        user.setUserId(userId);
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assign role
        user.setRoleList(List.of(AppConstants.ROLE_USER));
        
        logger.info(user.getProvider().toString());
        
        // 3. Save the new User first
        User savedUser = userRepo.save(user);

        // 4. SYNC: Immediately update any existing contacts that match this email
        // This fixes the bug: If User B has this email in their contacts, 
        // it now gets the new Name/Pic/Phone immediately.
        contactRepo.updateContactDetailsByEmail(
            savedUser.getName(), 
            savedUser.getPhoneNumber(), 
            savedUser.getProfilePic(), 
            savedUser.getEmail()
        );

        return savedUser;
    }
 
    @Override
    public Optional<User> getUserById(String id) {
      return userRepo.findById(id);
    }

    @Override
    public Optional<User> updateUser(User user) {
        User user1=userRepo.findById(user.getUserId()).orElseThrow(()->new ResourceNotFoundException("User not found!"));
        user1.setName(user.getName());
        user1.setAbout(user.getAbout());
        user1.setEmail(user.getEmail());
        user1.setPassword(user.getPassword());
        user1.setPhoneNumber(user.getPhoneNumber());
        user1.setProfilePic(user.getProfilePic());
        user1.setEnabled(user.isEnabled());
        user1.setEmailVerified(user.isEmailVerified());
        user1.setPhoneVerified(user.isPhoneVerified());
        user1.setProvider(user.getProvider());
        user1.setProviderUserId(user.getProviderUserId());
        User save=userRepo.save(user1);
        return Optional.ofNullable(save);
    }

    @Override
    public void deleteUser(String id) {
        User user1=userRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found!"));
        userRepo.delete(user1);
    }

    @Override
    public boolean isUserExist(String userId) {
        User user=userRepo.findById(userId).orElse(null);
        return user!=null ? true : false;
    }

    @Override
    public boolean isUserExistByEmail(String email) {
       User user=userRepo.findByEmail(email).orElse(null);
       return user!=null?true:false;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
    @Override
    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }
    @Override
    public User getUserByToken(String token) {
        return userRepo.findByEmailToken(token).orElse(null);
    }
}
