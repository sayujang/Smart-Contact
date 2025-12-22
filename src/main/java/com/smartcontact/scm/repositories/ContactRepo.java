package com.smartcontact.scm.repositories;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartcontact.scm.entities.Contact;
import com.smartcontact.scm.entities.User; 

public interface ContactRepo extends JpaRepository<Contact,String> {

    Page<Contact> findByUser(User user,Pageable pageable); //user is entity field in contact entity
    @Query("SELECT c FROM Contact c WHERE c.user.userId = :userId") //:parameterName represents parameter
    List<Contact> findByUserId(String userId);
    Page<Contact> findByNameContainingAndUser(String name, User user,Pageable pageable);
    Page<Contact> findByEmailContainingAndUser(String email,User user, Pageable pageable);
    Page<Contact> findByPhoneNumberContainingAndUser(String phoneNumber,User user, Pageable pageable);
    @Query("SELECT c.email FROM Contact c WHERE c.user.userId = :userId")
    List<String> findEmailsByUserId(String userId);
    boolean existsByUserAndEmail(User user, String email);
    List<Contact> findByEmail(String email);
}