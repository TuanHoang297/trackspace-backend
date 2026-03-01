package com.trackspace.auth;

import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Processes the authenticated Google OAuth2 user.
 * Finds an existing user by email, or auto-creates one with role TEAMMEMBER.
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

        String name = (String) attributes.get("name");
        userRepository.findByEmail(email).orElseGet(() -> createUser(email, name));

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
    }

    private User createUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name != null ? name : email);
        // OAuth2 users have no password — store an unguessable placeholder so
        // normal password-based login is impossible for this account.
        user.setPassword("OAUTH2_" + UUID.randomUUID());
        user.setRole(User.Role.TEAMMEMBER);
        user.setActive(true);
        return userRepository.save(user);
    }
}
