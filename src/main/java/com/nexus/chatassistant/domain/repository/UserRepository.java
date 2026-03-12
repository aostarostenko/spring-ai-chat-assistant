package com.nexus.chatassistant.domain.repository;

import com.nexus.chatassistant.domain.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Port for accessing user data stored in MongoDB.
 */
public interface UserRepository extends MongoRepository<User, String> {
    /**
     * Finds a user by their unique username for authentication.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their email for registration validation.
     */
    Optional<User> findByEmail(String email);
}