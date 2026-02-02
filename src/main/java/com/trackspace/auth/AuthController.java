package com.trackspace.auth;

import com.trackspace.common.ApiResponse;
import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * REST endpoints for authentication operations
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(
                ApiResponse.success(authResponse.getMessage(), authResponse)
        );
    }

    /**
     * Register endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(authResponse.getMessage(), authResponse)
        );
    }

    /**
     * Get current user endpoint
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get currently authenticated user information")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser() {
        User user = authService.getCurrentUser();
        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getActive()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin người dùng thành công", userInfo)
        );
    }

    /**
     * User Info DTO for /me endpoint
     */
    public record UserInfo(
            Long id,
            String email,
            String fullName,
            User.Role role,
            Boolean active
    ) {}
}
