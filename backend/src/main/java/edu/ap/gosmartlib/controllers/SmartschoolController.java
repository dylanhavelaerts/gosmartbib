package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
import edu.ap.gosmartlib.services.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/smartschool")
@RequiredArgsConstructor
public class SmartschoolController {

    private final UserDirectoryService userDirectoryService;

    @GetMapping("/users")
    public ResponseEntity<List<SmartschoolUserDTO>> searchSmartschoolUsers(
            @RequestParam String query,
            @AuthenticationPrincipal OAuth2User principal) {
        String actorUid = extractUid(principal);
        return ResponseEntity.ok(userDirectoryService.searchUsersForLoan(actorUid, query));
    }

    private String extractUid(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");
        }

        String uid = principal.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");
        }

        return uid;
    }
}