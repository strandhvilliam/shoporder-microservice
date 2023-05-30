package com.example.shopordermicrobackend.shopordermicrobackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Invalid auth token")
public class InvalidAuthException extends RuntimeException {

    public InvalidAuthException() {
        super("You are not authorized to access this data");
    }
}
