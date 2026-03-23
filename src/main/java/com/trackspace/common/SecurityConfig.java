package com.trackspace.common;

import com.trackspace.auth.CustomUserDetailsService;
import com.trackspace.auth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.trackspace.auth.JwtAuthenticationFilter;
import com.trackspace.auth.OAuth2AuthenticationFailureHandler;
import com.trackspace.auth.OAuth2AuthenticationSuccessHandler;
import com.trackspace.auth.OAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Security Configuration
 * Configures Spring Security for JWT-based authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@org.springframework.scheduling.annotation.EnableAsync
public class SecurityConfig {

        @Autowired
        private CustomUserDetailsService customUserDetailsService;

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private OAuth2UserService oAuth2UserService;

        @Autowired
        private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

        @Autowired
        private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

        @Bean
        public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
                return new HttpCookieOAuth2AuthorizationRequestRepository();
        }

        /**
         * Password encoder bean
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * Authentication provider
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(customUserDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        /**
         * Authentication manager bean
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        /**
         * Security filter chain
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                RequestMatcher apiRequestMatcher = new AntPathRequestMatcher("/api/**");

                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configure(http))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .defaultAuthenticationEntryPointFor((request, response, ex) -> {
                                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.setCharacterEncoding("UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"success\":false,\"message\":\"Unauthorized\",\"data\":null}");
                                                }, apiRequestMatcher)
                                                .defaultAccessDeniedHandlerFor((request, response, ex) -> {
                                                        response.setStatus(HttpStatus.FORBIDDEN.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.setCharacterEncoding("UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"success\":false,\"message\":\"Forbidden\",\"data\":null}");
                                                }, apiRequestMatcher))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/api-docs/**").permitAll()
                                                .requestMatchers("/api/v1/github/**").permitAll()
                                                .requestMatchers("/api/v1/jira/**").permitAll()
                                                .requestMatchers("/api/v1/events/**").permitAll()
                                                // Admin endpoints
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/users/**").hasRole("ADMIN")
                                                // Authenticated endpoints
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(endpoint -> endpoint
                                                                .baseUri("/oauth2/authorize")
                                                                .authorizationRequestRepository(
                                                                                cookieAuthorizationRequestRepository()))
                                                .redirectionEndpoint(redir -> redir
                                                                .baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(info -> info
                                                                .userService(oAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
