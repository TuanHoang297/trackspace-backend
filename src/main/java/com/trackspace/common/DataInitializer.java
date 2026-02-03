package com.trackspace.common;

import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data Initializer
 * Creates default admin account if not exists
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminIfNotExists();
    }

    /**
     * Create default admin account if no admin exists
     */
    private void createDefaultAdminIfNotExists() {
        // Check if any admin exists
        long adminCount = userRepository.count();
        
        if (adminCount == 0) {
            logger.info("No users found in database. Creating default admin account...");
            
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setRole(User.Role.ADMIN);
            admin.setActive(true);
            
            userRepository.save(admin);
            
            logger.info("=".repeat(70));
            logger.info("DEFAULT ADMIN ACCOUNT CREATED SUCCESSFULLY!");
            logger.info("Email: admin@gmail.com");
            logger.info("Password: admin123");
            logger.info("PLEASE CHANGE THE PASSWORD AFTER FIRST LOGIN!");
            logger.info("=".repeat(70));
        } else {
            logger.info("Database already contains users. Skipping default admin creation.");
        }
    }
}
