package com.demo_user_service.exceptions;

public class LoginAlreadyInUseException extends RuntimeException {
    public LoginAlreadyInUseException(String message){
        super(message);
    }
}
