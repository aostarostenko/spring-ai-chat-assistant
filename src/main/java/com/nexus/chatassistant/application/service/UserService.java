package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.User;
import com.nexus.chatassistant.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String email, String password) {
        log.info("Registering user: {}", username);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, encodedPassword, Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    public User updateEmail(String username, String newEmail) {
        log.info("Updating email for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (userRepository.findByEmail(newEmail).isPresent() && !user.email().equals(newEmail)) {
            throw new RuntimeException("Email already taken");
        }
        
        User updatedUser = user.withEmail(newEmail);
        return userRepository.save(updatedUser);
    }

    public void updatePassword(String username, String oldPassword, String newPassword) {
        log.info("Updating password for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.password())) {
            throw new RuntimeException("Incorrect current password");
        }

        User updatedUser = user.withPassword(passwordEncoder.encode(newPassword));
        userRepository.save(updatedUser);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
