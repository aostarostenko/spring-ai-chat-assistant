package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.exception.DaoException;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.exception.WebException;
import com.nexus.chatassistant.domain.model.User;
import com.nexus.chatassistant.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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
        User user;
        try {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
        }

        if (!passwordEncoder.matches(oldPassword, user.password())) {
            log.warn("Password update failed for user {}: Current password mismatch.", username);
            throw new SecurityException("Incorrect current password", ErrorCodes.CREDENTIAL_MISMATCH);
        }

        User updatedUser = user.withPassword(passwordEncoder.encode(newPassword));
        try {
            userRepository.save(updatedUser);
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_WRITE_FAILURE, e);
        }
        log.info("Password successfully updated for user: {}", username);
    }

    /**
     * Updates the email for an existing user after validating the new email is not already taken.
     */
    public User updateEmail(String username, String newEmail) {
        log.info("Updating email for user: {}", username);
        User user;
        try {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));

            if (userRepository.findByEmail(newEmail).isPresent() && !user.email().equals(newEmail)) {
                log.warn("Email update failed for {}: '{}' is already in use.", username, newEmail);
                throw new WebException("Duplicate check", ErrorCodes.DUPLICATE_USER);
            }
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
        }

        User updatedUser = user.withEmail(newEmail);
        try {
            return userRepository.save(updatedUser);
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_WRITE_FAILURE, e);
        }
    }

    /**
     * Registers a new user with BCrypt password encoding.
     */
    public User registerUser(String username, String email, String password) {
        log.info("Registering new user: {}", username);
        
        boolean exists;
        try {
            exists = userRepository.findByUsername(username).isPresent();
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
        }

        if (exists) {
            throw new WebException("Duplicate check", ErrorCodes.DUPLICATE_USER);
        }

        User user = new User(username, email, passwordEncoder.encode(password), Set.of("ROLE_USER"));
        try {
            return userRepository.save(user);
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_WRITE_FAILURE, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        try {
            return userRepository.findByUsername(username);
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
        }
    }
}