package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.exception.WebException;
import com.nexus.chatassistant.domain.model.User;
import com.nexus.chatassistant.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should encode password and save user during registration")
    void shouldRegisterUserSuccessfully() {
        // Given
        String username = "testuser";
        String fullName = "Test User";
        String email = "test@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User registeredUser = userService.registerUser(username, fullName, email, rawPassword);

        // Then
        assertThat(registeredUser.username()).isEqualTo(username);
        assertThat(registeredUser.fullName()).isEqualTo(fullName);
        assertThat(registeredUser.email()).isEqualTo(email);
        assertThat(registeredUser.password()).isEqualTo(encodedPassword);
        assertThat(registeredUser.roles()).containsExactly("ROLE_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DaoException during registration if database fails")
    void shouldThrowDaoExceptionDuringRegistrationIfDbFails() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenThrow(new org.springframework.dao.RecoverableDataAccessException("DB down"));

        // When / Then
        assertThatThrownBy(() -> userService.registerUser(username, "Test User", "email@test.com", "pass"))
                .isInstanceOf(com.nexus.chatassistant.domain.exception.DaoException.class)
                .hasMessageContaining("DB Error");
    }

    @Test
    @DisplayName("Should throw exception if username already exists during registration")
    void shouldThrowExceptionIfUsernameExists() {
        // Given
        String username = "existingUser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mock(User.class)));

        // When / Then
        assertThatThrownBy(() -> userService.registerUser(username, "Test User", "email@test.com", "pass"))
                .isInstanceOf(WebException.class)
                .hasMessage("Duplicate check");
    }

    @Test
    @DisplayName("Should throw SecurityException if user not found during password update")
    void shouldThrowSecurityExceptionWhenUserNotFoundDuringPasswordUpdate() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.updatePassword(username, "old", "new"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User check failed");
    }

    @Test
    @DisplayName("Should update password after successful verification")
    void shouldUpdatePasswordSuccessfully() {
        // Given
        String username = "testuser";
        String oldPass = "oldPass";
        String newPass = "newPass";
        String encodedNewPass = "encodedNewPass";
        User existingUser = new User("1", username, "Test User", "test@test.com", "encodedOldPass", Set.of("ROLE_USER"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(oldPass, "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode(newPass)).thenReturn(encodedNewPass);

        // When
        userService.updatePassword(username, oldPass, newPass);

        // Then
        verify(userRepository).save(argThat(user -> user.password().equals(encodedNewPass)));
    }

    @Test
    @DisplayName("Should throw exception if old password does not match during update")
    void shouldThrowExceptionIfOldPasswordMismatch() {
        // Given
        String username = "testuser";
        User existingUser = new User("1", username, "Test User", "test@test.com", "encodedOldPass", Set.of("ROLE_USER"));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPass", "encodedOldPass")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> userService.updatePassword(username, "wrongPass", "newPass"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Incorrect current password");
    }

    @Test
    @DisplayName("Should throw SecurityException if user not found during email update")
    void shouldThrowSecurityExceptionWhenUserNotFoundDuringEmailUpdate() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.updateEmail(username, "new@email.com"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User check failed");
    }

    @Test
    @DisplayName("Should update email successfully")
    void shouldUpdateEmailSuccessfully() {
        // Given
        String username = "testuser";
        String newEmail = "new@example.com";
        User existingUser = new User("1", username, "Test User", "old@example.com", "pass", Set.of("ROLE_USER"));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = userService.updateEmail(username, newEmail);

        // Then
        assertThat(updatedUser.email()).isEqualTo(newEmail);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception if email already taken during update")
    void shouldThrowExceptionIfEmailTaken() {
        // Given
        String username = "testuser";
        String newEmail = "taken@example.com";
        User existingUser = new User("1", username, "Test User", "old@example.com", "pass", Set.of("ROLE_USER"));
        User otherUser = new User("2", "other", "Other User", newEmail, "pass", Set.of("ROLE_USER"));
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(otherUser));

        // When / Then
        assertThatThrownBy(() -> userService.updateEmail(username, newEmail))
                .isInstanceOf(WebException.class)
                .hasMessage("Duplicate check");
    }

    @Test
    @DisplayName("Should update full name successfully")
    void shouldUpdateFullNameSuccessfully() {
        // Given
        String username = "testuser";
        String newFullName = "Updated Name";
        User existingUser = new User("1", username, "Old Name", "test@example.com", "pass", Set.of("ROLE_USER"));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = userService.updateFullName(username, newFullName);

        // Then
        assertThat(updatedUser.fullName()).isEqualTo(newFullName);
        verify(userRepository).save(any(User.class));
    }
}
