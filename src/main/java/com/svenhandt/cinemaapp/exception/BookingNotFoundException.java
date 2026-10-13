package com.svenhandt.cinemaapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Die Buchung existiert nicht.")
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException() {
        super("Die Buchung existiert nicht.");
    }
}
