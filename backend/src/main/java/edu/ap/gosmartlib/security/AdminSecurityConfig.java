package edu.ap.gosmartlib.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Spring Security configuratie voor het /admin/login-endpoint.
 * Registreert een aparte filter chain (@Order 1) die alleen overeenkomt met
 * /admin/login. Stelt BCrypt-verificatie, AdminLoginFilter en de bijhorende
 * CORS-regels in. Door @Order 1 wordt dit endpoint beoordeeld voor de algemene
 * OAuth2-filter chain.
 */
@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final AdminUserDetailsService adminUserDetailsService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    /**
     * Bouwt de security filter chain voor /admin/login.
     *
     * @param http de Spring Security HttpSecurity builder
     * @return de geconfigureerde SecurityFilterChain
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(adminUserDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder());

        ProviderManager authManager = new ProviderManager(provider);
        AdminLoginFilter loginFilter = new AdminLoginFilter(authManager, loginAttemptService);

        http
                .securityMatcher("/admin/login")
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(frontendUrl));
                    config.setAllowedMethods(List.of("POST", "OPTIONS"));
                    config.setAllowedHeaders(List.of("Content-Type"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
