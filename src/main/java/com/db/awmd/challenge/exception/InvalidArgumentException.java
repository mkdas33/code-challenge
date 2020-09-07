package com.db.awmd.challenge.exception;

public class InvalidArgumentException extends RuntimeException{

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
