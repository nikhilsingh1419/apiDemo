package com.example.apidemo.service;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}
