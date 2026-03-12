package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller responsible for handling user authentication and registration requests.
 * It serves the login and registration views and processes account creation.
 */
@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Renders the login page.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Renders the user registration form.
     */
    @GetMapping("/register")
    public String registerForm() {
        return "registration";
    }

    /**
     * Processes a new user registration request.
     * On success, redirects to the login page with a success flag.
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String password,
                               Model model) {
        log.info("Received registration request for username: {}", username);
        try {
            userService.registerUser(username, fullName, email, password);
            log.info("User {} successfully registered.", username);
            return "redirect:/login?registered";
        } catch (Exception e) {
            log.error("Registration failed for user {}: {}", username, e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "registration";
        }
    }
}