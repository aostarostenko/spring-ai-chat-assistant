package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.User;
import com.nexus.chatassistant.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Service for user account management, handling registration, lookups, and profile updates.
 * It coordinates secure data changes between the domain model and repository.
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Updates a user's password after verifying the current password matches.
     *
     * @param username    The username of the user to update.
     * @param oldPassword The current password for verification.
     * @param newPassword The new password to be encrypted and saved.
     */
    public void updatePassword(String username, String oldPassword, String newPassword) {
        log.info("Processing password update request for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.password())) {
            log.warn("Password update failed for user {}: Current password mismatch.", username);
            throw new RuntimeException("Incorrect current password");
        }

        User updatedUser = user.withPassword(passwordEncoder.encode(newPassword));
        userRepository.save(updatedUser);
        log.info("Password successfully updated for user: {}", username);
    }

    /**
     * Updates the email for an existing user after validating the new email is not already taken.
     */
    public User updateEmail(String username, String newEmail) {
        log.info("Updating email for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userRepository.findByEmail(newEmail).isPresent() && !user.email().equals(newEmail)) {
            log.warn("Email update failed for {}: '{}' is already in use.", username, newEmail);
            throw new RuntimeException("Email already taken");
        }

        User updatedUser = user.withEmail(newEmail);
        return userRepository.save(updatedUser);
    }

    /**
     * Registers a new user with BCrypt password encoding.
     */
    public User registerUser(String username, String email, String password) {
        log.info("Registering new user: {}", username);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User(username, email, passwordEncoder.encode(password), Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}