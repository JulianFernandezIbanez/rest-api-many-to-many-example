package com.example.exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class EmailNotFoundException extends UsernameNotFoundException {

    private static final long serialVersionUID = 1L;

    public EmailNotFoundException(String message) {
        super(message);
    }
}