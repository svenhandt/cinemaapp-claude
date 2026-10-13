package com.svenhandt.cinemaapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Die Vorstellung existiert nicht.")
public class ScreeningNotFoundException extends RuntimeException {

    public ScreeningNotFoundException() {
        super("Die Vorstellung existiert nicht.");
    }
}
