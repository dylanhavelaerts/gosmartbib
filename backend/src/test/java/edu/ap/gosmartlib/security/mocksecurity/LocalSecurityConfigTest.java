package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.repositories.schoolRepositories.SchoolRepository;
import edu.ap.gosmartlib.services.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LocalSecurityConfigTest {

    @Test
    void localCorsConfigurationSource_shouldAllowLocalFrontend() {
        LocalSecurityConfig config = new LocalSecurityConfig(mock(UserService.class), mock(SchoolRepository.class));

        CorsConfigurationSource source = config.localCorsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/books/all"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }
}
