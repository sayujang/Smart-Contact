package com.smartcontact.scm.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User; 

public interface ContactRepo extends JpaRepository<Contact,String> {

    List<Contact> findByUser(User user);
    @Query("SELECT c FROM Contact c WHERE c.user.userId = :userId") //:parameterName represents parameter
    List<Contact> findByUserId(String userId);}