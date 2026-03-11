package com.trackspace.auth;

import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Processes the authenticated Google OAuth2 user.
 * Only allows login if the email already exists in the database.
 * Auto-registration is intentionally disabled — accounts must be
 * pre-created by an administrator.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        userRepository.findByEmail(email).orElseThrow(() ->
                new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied"),
                        "Tài khoản của bạn không có quyền đăng nhập hệ thống"));

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
    }
}
