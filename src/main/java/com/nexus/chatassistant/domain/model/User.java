package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

/**
 * Represents an authenticated user in the system.
 * Managed as a MongoDB document in the 'users' collection.
 */
@Document(collection = "users")
public record User(
        @Id String id,
        String username,
        String fullName,
        String email,
        String password,
        Set<String> roles
) {
    /**
     * Standard constructor for creating a new user instance.
     */
    public User(String username, String fullName, String email, String password, Set<String> roles) {
        this(null, username, fullName, email, password, roles);
    }

    /**
     * Creates a new User instance with a modified email.
     */
    public User withEmail(String newEmail) {
        return new User(id, username, fullName, newEmail, password, roles);
    }

    /**
     * Creates a new User instance with a modified (encoded) password.
     */
    public User withPassword(String newPassword) {
        return new User(id, username, fullName, email, newPassword, roles);
    }

    /**
     * Creates a new User instance with a modified fullName.
     */
    public User withFullName(String newFullName) {
        return new User(id, username, newFullName, email, password, roles);
    }
}