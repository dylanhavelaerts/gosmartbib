package edu.ap.gosmartlib.security;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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

/**
 * Productieconfiguratie voor Spring Security.
 *
 * Deze configuratie gebruikt echte Smartschool OAuth2-authenticatie. Alleen de
 * login-, OAuth2- en error-endpoints zijn publiek. Alle andere backendroutes
 * vereisen een geldige sessie. Method security blijft daarnaast actief voor
 * fijnmazige rolcontrole via @PreAuthorize en RoleGuard.
 */

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
        private final OAuth2UserService oAuth2UserService;

        @Value("${app.frontend.base-url}")
        private String frontendUrl;

        /**
         * Bouwt de security filter chain voor productie.
         *
         * De filter chain configureert CORS met credentials, schakelt CSRF uit voor de
         * huidige JSON/API-flow, beschermt alle niet-publieke endpoints en koppelt de
         * OAuth2-login aan de custom OAuth2UserService, PKCE-disabled resolver en
         * success handler.
         *
         * CSRF wordt hier bewust uitgeschakeld omdat de frontend momenteel geen
         * CSRF-token
         * meestuurt bij API-calls. Omdat de applicatie wel sessiecookies gebruikt,
         * blijft
         * dit een security trade-off die gecompenseerd wordt met
         * SameSite/Secure/HttpOnly
         * cookies, strikte CORS-configuratie en backendautorisatie.
         *
         * @param http de Spring Security HttpSecurity builder
         * @return de geconfigureerde SecurityFilterChain
         * @throws Exception wanneer Spring Security de filter chain niet kan bouwen
         */

        @Bean
        @Order(2)
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // CSRF is uitgeschakeld omdat de frontend momenteel geen Spring CSRF-token
                                // meestuurt bij JSON/API-calls. Omdat de app wel sessiecookies gebruikt,
                                // wordt dit gecompenseerd met strikte CORS, SameSite/Secure/HttpOnly cookies,
                                // HTTPS in productie en backendautorisatie.
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
                                                                .userService(oAuth2UserService))
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

        /**
         * CORS-configuratie voor communicatie tussen frontend en backend.
         *
         * allowCredentials staat aan omdat de browser de sessiecookies moet meesturen
         * bij calls zoals /auth/me. De allowed origin moet exact overeenkomen met de
         * frontend-URL, anders worden cookies door de browser geweigerd.
         *
         * @return CORS-configuratie voor alle backendroutes
         */

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendUrl));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}