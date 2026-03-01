package com.trackspace.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Stores the OAuth2 authorization request in an HTTP cookie instead of the HTTP session,
 * keeping the architecture stateless and compatible with JWT.
 *
 * <p>Cookie notes:
 * <ul>
 *   <li>Base64 uses {@code withoutPadding()} to avoid {@code =} characters that break cookie values.</li>
 *   <li>{@code SameSite=Lax} is set via raw header because the Jakarta {@link Cookie} API
 *       does not expose the SameSite attribute.</li>
 * </ul>
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE = "OAUTH2_AUTH_REQUEST";
    public static final String REDIRECT_URI_COOKIE = "REDIRECT_URI";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookieValue(request, OAUTH2_AUTH_REQUEST_COOKIE)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE);
            deleteCookie(request, response, REDIRECT_URI_COOKIE);
            return;
        }

        addCookie(response, OAUTH2_AUTH_REQUEST_COOKIE, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS);

        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isBlank()) {
            addCookie(response, REDIRECT_URI_COOKIE, redirectUri, COOKIE_MAX_AGE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE);
        return authRequest;
    }

    // -------------------------------------------------------------------------
    // Cookie helpers
    // -------------------------------------------------------------------------

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        response.addHeader("Set-Cookie",
                name + "=" + value + "; Path=/; Max-Age=" + maxAge + "; SameSite=Lax");
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        boolean exists = Arrays.stream(cookies).anyMatch(c -> c.getName().equals(name));
        if (exists) {
            response.addHeader("Set-Cookie", name + "=; Path=/; Max-Age=0; SameSite=Lax");
        }
    }

    @SuppressWarnings("deprecation")
    private String serialize(OAuth2AuthorizationRequest request) {
        // withoutPadding() avoids '=' characters that break cookie parsing
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SerializationUtils.serialize(request));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                    Base64.getUrlDecoder().decode(value));
        } catch (Exception e) {
            return null;
        }
    }
}
