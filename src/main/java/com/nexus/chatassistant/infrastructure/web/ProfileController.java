package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.UserService;
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

@Controller
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/update-email")
    public String updateEmail(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String email,
                              Model model) {
        try {
            userService.updateEmail(userDetails.getUsername(), email);
            return "redirect:/profile?emailUpdated";
        } catch (Exception e) {
            log.error("Email update error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return profile(userDetails, model);
        }
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 Model model) {
        try {
            userService.updatePassword(userDetails.getUsername(), oldPassword, newPassword);
            return "redirect:/profile?passwordUpdated";
        } catch (Exception e) {
            log.error("Password update error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return profile(userDetails, model);
        }
    }
}
