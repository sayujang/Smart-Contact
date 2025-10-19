package com.smartcontact.scm.forms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserForm {
    private String name;
    private String password;
    private String email;
    private String about;
    private String phoneNumber;
    @Override
    public String toString() {
        return "UserForm [name=" + name + ", password=" + password + ", email=" + email + ", about=" + about
                + ", phoneNumber=" + phoneNumber + "]";
    }
    
}
