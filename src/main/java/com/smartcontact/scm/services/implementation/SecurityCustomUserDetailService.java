package com.smartcontact.scm.services.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smartcontact.scm.repositories.UserRepo;

//function to tell the spring security how to access the userrecord in database during login on the basis of email
@Service
public class SecurityCustomUserDetailService implements UserDetailsService{
    @Autowired
    private UserRepo userRepo;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        return userRepo.findByEmail(username).orElseThrow(()->new UsernameNotFoundException("User not found with given email"));
    }
    
}
