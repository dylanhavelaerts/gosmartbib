package edu.ap.gosmartlib.security;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("prod") // Security config voor server in productie, met echte Smartschool OAuth2 login
                 // en CORS ingesteld voor frontend
@Slf4j
public class SecurityConfig {

        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final OAuth2AuthorizationRequestResolver pkceDisabledResolver;
        private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
        private final OAuth2UserService OAuth2UserService;

        @Value("${app.frontend.base-url}")
        private String frontendUrl;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/public/**",
                                                                "/auth/login",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/login/**",
                                                                "/error")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(OAuth2UserService))
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestResolver(pkceDisabledResolver)
                                                                .authorizationRequestRepository(
                                                                                authorizationRequestRepository))
                                                .failureHandler((request, response, exception) -> {
                                                        log.error("OAuth2 login callback failed", exception);
                                                        response.sendRedirect(frontendUrl + "/login?error=true");
                                                })
                                                .successHandler(oAuth2LoginSuccessHandler));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendUrl));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}