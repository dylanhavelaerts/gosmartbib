package edu.ap.gosmartlib.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private SecurityConfig buildConfig() {
        OAuth2LoginSuccessHandler successHandler = mock(OAuth2LoginSuccessHandler.class);
        OAuth2AuthorizationRequestResolver resolver = mock(OAuth2AuthorizationRequestResolver.class);
        @SuppressWarnings("unchecked")
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> repository = mock(
                AuthorizationRequestRepository.class);
        OAuth2UserService oauth2UserService = mock(OAuth2UserService.class);

        SecurityConfig config = new SecurityConfig(successHandler, resolver, repository, oauth2UserService);
        ReflectionTestUtils.setField(config, "frontendUrl", "http://localhost:3000");
        return config;
    }

    @Test
    void corsConfigurationSource_shouldContainFrontendOriginAndExpectedMethods() {
        SecurityConfig config = buildConfig();

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/books/all"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }
}
