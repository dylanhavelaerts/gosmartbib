package edu.ap.gosmartlib.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidUserException extends RuntimeException {
    public InvalidUserException(HttpStatus code, String message) {
        super(message);
    }
}
