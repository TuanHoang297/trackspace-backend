package com.trackspace.common;

/**
 * Thrown when an authenticated user attempts to access a resource
 * they do not have permission to view (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
