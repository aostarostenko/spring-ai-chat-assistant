package com.nexus.chatassistant.infrastructure.security;

import com.nexus.chatassistant.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Infrastructure Adapter that loads user data from MongoDB to be used by Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Spring Security attempting to load user: {}", username);

        com.nexus.chatassistant.domain.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User '{}' not found in database.", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        log.info("User '{}' successfully loaded with roles: {}", username, user.roles());

        return new org.springframework.security.core.userdetails.User(
                user.username(),
                user.password(),
                user.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );
    }
}