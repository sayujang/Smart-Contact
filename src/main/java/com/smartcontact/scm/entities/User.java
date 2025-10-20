package com.smartcontact.scm.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="users")
//this implements userdetails to give spring security the details of username(email in this case) password and roles for authentication
public class User implements UserDetails{
    @Id
    private String userId;
    @Column(name="user_name",nullable =false)
    private String name;
    @Column(unique = true,nullable = false)
    private String email;
    @Getter(value=AccessLevel.NONE)
    private String password;
    @Column(length=10000)
    private String about;
    @Column(length = 10000)
    private String profilePic;
    private String phoneNumber;
    @Getter(value=AccessLevel.NONE)
    private boolean enabled=true;
    private boolean emailVerified=false;
    private boolean phoneVerified=false;
    @Enumerated(value = EnumType.STRING)//tells jpa how to save enums in database
    private Providers provider=Providers.SELF;
    private String providerUserId;

    //Mapping relation between contact table
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch=FetchType.LAZY,orphanRemoval = true)//here orphan removal= true makes it possible to remove a row from contact table in the database as well(not only in in-memory) when user deletes one of his contacts casdetypeall means if one user(parent) is deleted/saved all his contacts are deleted/saved
    private List<Contact> contacts=new ArrayList<>();

    //creates a new table as each user could have multiple roles
    @ElementCollection(fetch=FetchType.EAGER)
    private List<String> roleList=new ArrayList<>();
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<SimpleGrantedAuthority> roles=roleList.stream().map(role->new SimpleGrantedAuthority(role)).collect(Collectors.toList());
        return roles;
    }
    @Override
    public String getUsername() {
        return this.email;
    }
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    @Override
    public String getPassword() {
        return this.password;
    }
   
}
