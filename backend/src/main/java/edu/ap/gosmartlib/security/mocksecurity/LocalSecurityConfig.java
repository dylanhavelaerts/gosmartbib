package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Spring Security configuratie voor lokale ontwikkeling.
 *
 * Alle endpoints zijn publiek toegankelijk. In plaats van OAuth2 wordt een
 * MockAuth-filter gebruikt die automatisch een gesimuleerde gebruiker injecteert.
 * CORS is ingesteld voor localhost:3000.
 */
@Configuration
@Profile("local") // Security config voor lokaal, met mock auth en geen CORS
@EnableMethodSecurity
@RequiredArgsConstructor
public class LocalSecurityConfig {

    private final UserService userService;
    private final SchoolRepository schoolRepository;

    /**
     * Bouwt de lokale filter chain met MockAuth en open autorisatie.
     *
     * @param http de Spring Security HttpSecurity builder
     * @return de geconfigureerde SecurityFilterChain
     * @throws Exception wanneer Spring Security de filter chain niet kan bouwen
     */
    @Bean
    public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(localCorsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .addFilterBefore(new MockAuth(userService, schoolRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS-configuratie voor lokale ontwikkeling.
     *
     * @return CORS-configuratie die localhost:3000 toelaat
     */
    @Bean
    public CorsConfigurationSource localCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
