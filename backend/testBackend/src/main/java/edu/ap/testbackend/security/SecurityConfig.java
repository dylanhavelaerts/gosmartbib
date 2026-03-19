package edu.ap.testbackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("prod") // Security config voor server in productie, met echte Smartschool OAuth2 login en CORS ingesteld voor frontend
public class SecurityConfig {

        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final OAuth2AuthorizationRequestResolver pkceDisabledResolver;

        @Value("${app.frontend.base-url}")
        private String frontendUrl;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .csrf(csrf -> csrf.disable())
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/public/**", "/auth/login").permitAll()
                                .anyRequest().authenticated()
                        )
                        .oauth2Login(oauth2 -> oauth2
                                .authorizationEndpoint(auth -> auth
                                        .authorizationRequestResolver(pkceDisabledResolver)
                                )
                                .successHandler(oAuth2LoginSuccessHandler)
                        );

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendUrl)); // sta nextjs url toe
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true); // voor cookies te laten werken

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}