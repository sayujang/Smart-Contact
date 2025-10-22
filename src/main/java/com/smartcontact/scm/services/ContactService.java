package com.smartcontact.scm.services;

import java.util.List;

import com.smartcontact.scm.entities.Contact;

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
    
}
