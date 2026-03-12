package com.nexus.chatassistant.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Configuration for Internationalization (i18n).
 * Manages how the application detects and switches languages.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    /**
     * Defines the strategy for storing the user's locale.
     * Uses a Session-based approach with Ukrainian as the default.
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(new Locale("uk"));
        return slr;
    }

    /**
     * Interceptor that listens for a 'lang' parameter in the URL.
     * Example: http://localhost:8080/profile?lang=uk
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    /**
     * Registers the interceptor into the Spring MVC lifecycle.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}