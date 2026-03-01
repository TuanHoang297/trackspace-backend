package com.trackspace.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

/**
 * Shared cookie utilities for OAuth2 handlers.
 * Centralises cookie read/clear logic used by both success and failure handlers.
 */
public final class OAuth2CookieUtils {

    private OAuth2CookieUtils() {}

    /** Returns the value of the named cookie, or empty if not present. */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    /** Expires the OAuth2 auth-request and redirect-URI cookies after the flow completes. */
    public static void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> c.getName().equals(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE)
                        || c.getName().equals(HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_COOKIE))
                .forEach(c -> {
                    c.setValue("");
                    c.setPath("/");
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }
}
