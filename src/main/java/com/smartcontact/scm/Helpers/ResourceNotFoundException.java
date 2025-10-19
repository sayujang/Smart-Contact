package com.smartcontact.scm.Helpers;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message)
    {
        super(message);//custome message
    }
    public ResourceNotFoundException()
    {
        super("Resource not found!");//default message
    }
    
}
