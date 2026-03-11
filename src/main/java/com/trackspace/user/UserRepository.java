package com.trackspace.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository
 * JPA Repository for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email
     * @param email User email
     * @return Optional User
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if email exists
     * @param email User email
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by GitHub login (username)
     * @param githubLogin GitHub username
     * @return Optional User
     */
    Optional<User> findByGithubLogin(String githubLogin);

    /**
     * Find user by full name (case-insensitive) — used as fallback for Jira displayName matching
     */
    Optional<User> findFirstByFullNameIgnoreCase(String fullName);
}
