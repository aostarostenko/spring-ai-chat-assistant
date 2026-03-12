package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling user profile views and settings modifications.
 */
@Controller
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the user profile page.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));
        model.addAttribute("user", user);
        return "profile";
    }

    /**
     * Handles the request to change a user's password.
     */
    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 Model model) {
        try {
            log.info("User '{}' requested a password change.", userDetails.getUsername());
            userService.updatePassword(userDetails.getUsername(), oldPassword, newPassword);
            return "redirect:/profile?passwordUpdated";
        } catch (Exception e) {
            log.error("Password update error for user '{}': {}", userDetails.getUsername(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return profile(userDetails, model);
        }
    }

    /**
     * Handles the request to update a user's email.
     */
    @PostMapping("/profile/update-email")
    public String updateEmail(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String email,
                              Model model) {
        try {
            log.info("User '{}' updating email to: {}", userDetails.getUsername(), email);
            userService.updateEmail(userDetails.getUsername(), email);
            return "redirect:/profile?emailUpdated";
        } catch (Exception e) {
            log.error("Email update error for user '{}': {}", userDetails.getUsername(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return profile(userDetails, model);
        }
    }

    /**
     * Handles the request to update a user's full name.
     */
    @PostMapping("/profile/update-fullname")
    public String updateFullName(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String fullName,
                                 Model model) {
        try {
            log.info("User '{}' updating full name to: {}", userDetails.getUsername(), fullName);
            userService.updateFullName(userDetails.getUsername(), fullName);
            return "redirect:/profile?fullNameUpdated";
        } catch (Exception e) {
            log.error("Full name update error for user '{}': {}", userDetails.getUsername(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return profile(userDetails, model);
        }
    }
}