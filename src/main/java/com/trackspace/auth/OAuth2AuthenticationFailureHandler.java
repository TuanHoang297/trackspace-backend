package com.trackspace.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles failed Google OAuth2 authentication.
 * Redirects to the frontend with {@code ?error=...}.
 *
 * <p>Error messages from Spring OAuth2 are wrapped in square brackets
 * (e.g. {@code [authorization_request_not_found]}), which are invalid
 * characters in an HTTP request-target (RFC 3986) and cause Tomcat to
 * reject the request. The brackets are stripped before redirecting.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String targetUrl = OAuth2CookieUtils
                .getCookieValue(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_COOKIE)
                .orElse(authorizedRedirectUri());

        String errorMessage = stripBrackets(exception.getLocalizedMessage());

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorMessage)
                .build().encode().toUriString();

        OAuth2CookieUtils.clearAuthCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /** Returns a safe fallback redirect target that the frontend can handle. */
    private String authorizedRedirectUri() {
        return "/login-error";
    }

    /**
     * Spring OAuth2 wraps error codes in brackets, e.g. {@code [authorization_request_not_found]}.
     * Strip them so the value is safe to embed in a URL query parameter.
     */
    private String stripBrackets(String message) {
        if (message != null && message.startsWith("[") && message.endsWith("]")) {
            return message.substring(1, message.length() - 1);
        }
        return message;
    }
}
