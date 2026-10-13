package com.svenhandt.cinemaapp.exception;

public class InvalidCardNumberException extends RuntimeException {

    public InvalidCardNumberException(String message) {
        super(message);
    }
}
