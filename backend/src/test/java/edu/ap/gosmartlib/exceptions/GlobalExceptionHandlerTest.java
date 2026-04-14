package edu.ap.gosmartlib.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void givenAlreadyReviewedException_whenHandleAlreadyReviewed_thenReturnsConflictWithMessage() {
        AlreadyReviewedException exception = new AlreadyReviewedException("Je hebt dit boek al beoordeeld");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleAlreadyReviewed(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Je hebt dit boek al beoordeeld", response.getBody().get("message"));
    }

    @Test
    void givenOutOfBoundsException_whenHandleOutOfBounds_thenReturnsBadRequestWithMessage() {
        OutOfBoundsException exception = new OutOfBoundsException("Rating moet tussen 0 en 5 liggen");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleOutOfBounds(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Rating moet tussen 0 en 5 liggen", response.getBody().get("message"));
    }

    @Test
    void givenSecurityException_whenHandleSecurity_thenReturnsForbiddenWithMessage() {
        SecurityException exception = new SecurityException("Je kan enkel je eigen reviews bewerken");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleSecurity(exception);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Je kan enkel je eigen reviews bewerken", response.getBody().get("message"));
    }

    @Test
    void givenEntityNotFoundException_whenHandleEntityNotFound_thenReturnsNotFoundWithMessage() {
        EntityNotFoundException exception = new EntityNotFoundException("Review niet gevonden");

        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleEntityNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Review niet gevonden", response.getBody().get("message"));
    }
}
