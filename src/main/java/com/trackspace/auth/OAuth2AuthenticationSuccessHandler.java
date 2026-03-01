package com.trackspace.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Handles successful Google OAuth2 authentication.
 * Generates a JWT token and redirects to the frontend with {@code ?token=...}.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.authorized-redirect-uris}")
    private List<String> authorizedRedirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);
        OAuth2CookieUtils.clearAuthCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        // Use the redirect_uri supplied by the frontend, falling back to the first
        // configured URI to avoid redirecting to the backend root ("/") which would
        // cause an auth loop.
        String targetUrl = OAuth2CookieUtils
                .getCookieValue(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_COOKIE)
                .filter(this::isAuthorizedRedirectUri)
                .orElse(authorizedRedirectUris.isEmpty() ? getDefaultTargetUrl() : authorizedRedirectUris.get(0));

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateTokenFromEmail(oAuth2User.getAttribute("email"));

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();
    }

    private boolean isAuthorizedRedirectUri(String redirectUri) {
        URI client = URI.create(redirectUri);
        return authorizedRedirectUris.stream().anyMatch(authorizedUri -> {
            URI authorized = URI.create(authorizedUri);
            return authorized.getHost().equalsIgnoreCase(client.getHost())
                    && authorized.getPort() == client.getPort();
        });
    }
}
