package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Set;

@Document(collection = "users")
public record User(
    @Id String id,
    String username,
    String email,
    String password,
    Set<String> roles
) {
    public User(String username, String email, String password, Set<String> roles) {
        this(null, username, email, password, roles);
    }

    public User withEmail(String newEmail) {
        return new User(id, username, newEmail, password, roles);
    }

    public User withPassword(String newPassword) {
        return new User(id, username, email, newPassword, roles);
    }
}
