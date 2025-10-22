package com.smartcontact.scm.forms;

import org.springframework.web.multipart.MultipartFile;

import com.smartcontact.scm.Validators.ValidFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ContactForm {
    @NotBlank(message="Name is required")
    private String name;
    @NotBlank(message="Email is required")
    @Email(message="Invalid Email Address")
    private String email;
    @NotBlank(message="Phone Number is required")
    @Pattern(regexp="^\\+?[0-9]{8,15}$", message="Invalid Phone Number")
    private String phoneNumber;
    @NotBlank(message="Address is required")
    private String address;
    private String description;
    private String websiteLink;
    private String linkedInLink;
    private boolean favorite;
    @ValidFile
    private MultipartFile contactPic;

}
