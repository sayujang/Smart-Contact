package com.smartcontact.scm.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message="Username is required")
    @Size(min=3, message="Min 3 characters are required")
    private String name;
    @NotBlank(message = "Password is required")
    @Size(min=6, message="Min 6 characters is required")
    private String password;
    @Email(message = "Invalid Email Address")
    private String email;
    @NotBlank(message = "About is required")
    private String about;
    @Pattern(regexp="^\\+?[0-9]{8,15}$", message="Invalid Phone Number")
    private String phoneNumber;
    @Override
    public String toString() {
        return "UserForm [name=" + name + ", password=" + password + ", email=" + email + ", about=" + about
                + ", phoneNumber=" + phoneNumber + "]";
    }
    
}
