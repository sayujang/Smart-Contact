package com.smartcontact.scm.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User;

public interface ContactService {
    Contact save(Contact contact);

    // update contact
    Contact update(Contact contact);

    // get contacts
    List<Contact> getAll();

    // get contact by id

    Contact getById(String id);

    // delete contact

    void delete(String id);
    List<Contact> getByUserId(String userId);
    Page<Contact> getByUser(User user,int page,int size,String sortBy,String sortDir);
    
}
