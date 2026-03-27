package com.nexus.chatassistant.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import com.nexus.chatassistant.domain.exception.*;
import com.nexus.chatassistant.domain.exception.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    @Value("${app.features.multi-language-enabled:true}")
    private boolean multiLangEnabled;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ModelAttribute("isMultiLangEnabled")
    public boolean isMultiLangEnabled() {
        return multiLangEnabled;
    }

    /**
     * Handles Security Errors.
     * Usually results in a redirect to login or an access-denied page.
     */
    @ExceptionHandler(SecurityException.class)
    public String handleSecurityException(SecurityException ex, RedirectAttributes redirectAttributes) {
        String msg = messageSource.getMessage(ex.getCode(), null, LocaleContextHolder.getLocale());
        log.error("SECURITY ALERT [{}]: {}", ex.getCode(), ex.getMessage());
        redirectAttributes.addFlashAttribute("securityError", msg);
        return "redirect:/login?error=" + ex.getCode();
    }

    @ExceptionHandler(DaoException.class)
    public String handleDao(DaoException ex, RedirectAttributes ra) {
        // Log the full technical error for debugging
        log.error("Database Error [Code: {}]: {}", ex.getCode(), ex.getMessage(), ex.getCause());

        // Resolve the user-friendly message from properties
        String msg = messageSource.getMessage(ex.getCode(), null, "A data error occurred.", LocaleContextHolder.getLocale());

        ra.addFlashAttribute("error", msg);
        return "redirect:/"; // Usually redirect to home if the DB fails
    }

    @ExceptionHandler(WebException.class)
    public String handleWebException(WebException ex, RedirectAttributes redirectAttributes) {
        String msg = messageSource.getMessage(ex.getCode(), null, LocaleContextHolder.getLocale());
        log.warn("Business Logic Error: {}", msg);
        redirectAttributes.addFlashAttribute("error", msg);
        return "redirect:/profile";
    }

    /**
     * Specifically handle missing static resources to prevent log noise.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public void handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        // We intentionally leave this empty to "swallow" the error,
        // or you can log it as a simple DEBUG message.
        log.debug("Resource not found: {}", ex.getResourcePath());
    }

    /**
     * Fallback for generic system crashes.
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Critical System Failure: ", ex);
        model.addAttribute("error", "An unexpected system error occurred.");
        return "error";
    }
}