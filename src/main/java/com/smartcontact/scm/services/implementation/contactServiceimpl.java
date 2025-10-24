package com.smartcontact.scm.services.implementation;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.smartcontact.scm.Helpers.ResourceNotFoundException;
import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User;
import com.smartcontact.scm.repositories.ContactRepo;
import com.smartcontact.scm.services.ContactService;
@Service
public class contactServiceimpl implements ContactService {

    @Autowired
    ContactRepo contactRepo;
    @Override
    public Contact save(Contact contact) {
        String contactId=UUID.randomUUID().toString();
        contact.setId(contactId);
        return contactRepo.save(contact);
    }

    @Override
    public Contact update(Contact contact) {
        var contactOld = contactRepo.findById(contact.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        contactOld.setName(contact.getName());
        contactOld.setEmail(contact.getEmail());
        contactOld.setPhoneNumber(contact.getPhoneNumber());
        contactOld.setAddress(contact.getAddress());
        contactOld.setDescription(contact.getDescription());
        contactOld.setPicture(contact.getPicture());
        contactOld.setFavorite(contact.isFavorite());
        contactOld.setWebsiteLink(contact.getWebsiteLink());
        contactOld.setLinkedInLink(contact.getLinkedInLink());
        contactOld.setCloudinaryPublicId(contact.getCloudinaryPublicId());

        return contactRepo.save(contactOld);
    }

    @Override
    public List<Contact> getAll() {
        return contactRepo.findAll();
    }

    @Override
    public Contact getById(String id) {
       return contactRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Contact not found with given id: "+id));
    }

    @Override
    public void delete(String id) {
        Contact contact=contactRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Contact not found with given id: "+id));
        contactRepo.delete(contact);
    }

    @Override
    public Page<Contact> getByUser(User user, int page, int size, String sortBy, String sortDir) {
    Sort sort=sortDir.equals("desc")?Sort.by(sortBy).descending():Sort.by(sortBy).ascending();
     var pageable=PageRequest.of(page,size,sort);
     return contactRepo.findByUser(user,pageable);   
    }

    @Override
    public Page<Contact> searchByName(String name, int page, int size, String sortBy, String sortDir,User user) {
        Sort sort=sortDir.equals("desc")?Sort.by(sortBy).descending():Sort.by(sortBy).ascending();
        var pageable=PageRequest.of(page,size,sort);
        return contactRepo.findByNameContainingAndUser(name,user, pageable);
    }

    @Override
    public Page<Contact> searchByEmail(String email, int page, int size, String sortBy, String sortDir, User user) {
        Sort sort=sortDir.equals("desc")?Sort.by(sortBy).descending():Sort.by(sortBy).ascending();
        var pageable=PageRequest.of(page,size,sort);
        return contactRepo.findByEmailContainingAndUser(email,user, pageable);
    }

    @Override
    public Page<Contact> searchByPhoneNumber(String phoneNumber, int page, int size, String sortBy, String sortDir, User user) {
        Sort sort=sortDir.equals("desc")?Sort.by(sortBy).descending():Sort.by(sortBy).ascending();
        var pageable=PageRequest.of(page,size,sort);
        return contactRepo.findByPhoneNumberContainingAndUser(phoneNumber,user, pageable);
    }

   

  

}
