package com.trackspace.auth;

import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes OAuth2 users from Google and GitHub.
 * Only allows login if the account already exists in the database.
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
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("github".equals(registrationId)) {
            return processGithubUser(oAuth2User, attributes);
        }

        // Default: Google (email attribute is always present)
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

    /**
     * Handles GitHub OAuth2 login.
     * Finds user by GitHub email. If matched and githubLogin not yet set, saves the GitHub username.
     */
    private OAuth2User processGithubUser(OAuth2User oAuth2User, Map<String, Object> attributes) {
        String githubLogin = (String) attributes.get("login");
        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "Tài khoản GitHub của bạn không có email công khai. Vui lòng công khai email trên GitHub và thử lại.");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied"),
                        "Tài khoản của bạn không có quyền đăng nhập hệ thống"));

        // Auto-link GitHub username if not yet set
        if ((user.getGithubLogin() == null || user.getGithubLogin().isBlank())
                && githubLogin != null && !githubLogin.isBlank()) {
            user.setGithubLogin(githubLogin);
            userRepository.save(user);
        }

        Map<String, Object> modifiedAttributes = new HashMap<>(attributes);
        modifiedAttributes.put("email", user.getEmail());
        return new DefaultOAuth2User(oAuth2User.getAuthorities(), modifiedAttributes, "email");
    }
}
