package com.smartcontact.scm.Validators;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileValidator implements ConstraintValidator<ValidFile,MultipartFile>{

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 5 MB
        if(value==null || value.isEmpty()){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File cannot be empty").addConstraintViolation();
            return false;
        }
        if(value.getSize()>MAX_FILE_SIZE){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File size exceeds the maximum limit of 5 MB").addConstraintViolation();
            return false;
        }
        return true;
    }
    
}
