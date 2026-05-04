package edu.ap.gosmartlib.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthHelper {

    public String extractUid(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");
        }
        String uid = principal.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");
        }
        return uid;
    }

    public String extractUidOrNull(OAuth2User principal) {
        if (principal == null) return null;
        String uid = principal.getAttribute("userID");
        return (uid == null || uid.isBlank()) ? null : uid;
    }
}
