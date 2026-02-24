package com.trackspace.admin;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service
 * Handles admin operations for user management
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_NOT_FOUND_MESSAGE = "Không tìm thấy người dùng với ID: %d";
    private static final String ACCOUNT_CREATED_MESSAGE = "Tạo tài khoản thành công";

    /**
     * Create user account for lecturer, teamleader, or teammember
     *
     * @param request User creation request
     * @return Created user response
     * @throws BadRequestException if role is ADMIN or email already exists
     */
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        validateRoleNotAdmin(request.getRole());
        validateEmailNotExists(request.getEmail());

        User user = buildUserFromRequest(request);
        User savedUser = userRepository.save(user);

        return buildCreateUserResponse(savedUser);
    }

    /**
     * Get all users in the system
     *
     * @return List of all users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::buildUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update user status (activate/deactivate)
     *
     * @param userId User ID to update
     * @param active New active status
     * @return Updated user response
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public UserResponse updateUserStatus(Long userId, Boolean active) {
        User user = findUserById(userId);
        user.setActive(active);
        User updatedUser = userRepository.save(user);
        return buildUserResponse(updatedUser);
    }

    /**
     * Delete user account
     *
     * @param userId User ID to delete
     * @throws ResourceNotFoundException if user not found
     * @throws BadRequestException if trying to delete admin account
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        validateRoleNotAdmin(user.getRole());
        userRepository.delete(user);
    }

    // ==================== Helper Methods ====================

    /**
     * Find user by ID
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(USER_NOT_FOUND_MESSAGE, userId)));
    }

    /**
     * Validate role is not ADMIN
     */
    private void validateRoleNotAdmin(User.Role role) {
        if (role == User.Role.ADMIN) {
            throw new BadRequestException("Không thể tạo hoặc xóa tài khoản Admin thông qua API này");
        }
    }

    /**
     * Validate email does not exist
     */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã tồn tại trong hệ thống");
        }
    }

    /**
     * Build User entity from request
     */
    private User buildUserFromRequest(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setActive(true);
        return user;
    }

    /**
     * Build CreateUserResponse from User entity
     */
    private CreateUserResponse buildCreateUserResponse(User user) {
        return CreateUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .message(ACCOUNT_CREATED_MESSAGE)
                .build();
    }

    /**
     * Build UserResponse from User entity
     */
    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
