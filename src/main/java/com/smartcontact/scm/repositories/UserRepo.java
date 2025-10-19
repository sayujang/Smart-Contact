package com.smartcontact.scm.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcontact.scm.entities.User;

public interface UserRepo extends JpaRepository<User,String> {

    Optional<User> findByEmail(String email);
}
