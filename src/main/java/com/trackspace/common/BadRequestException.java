package com.trackspace.common;

/**
 * Bad Request Exception
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
