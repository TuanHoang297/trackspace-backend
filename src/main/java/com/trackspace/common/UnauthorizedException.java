package com.trackspace.common;

/**
 * Unauthorized Exception
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
