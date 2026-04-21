package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.services.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserDirectoryService userDirectoryService;

    @PostMapping("/display-names")
    @PreAuthorize("isAuthenticated()")
    public ResolveDisplayNamesResponse resolveDisplayNames(
            @RequestBody ResolveDisplayNamesRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return userDirectoryService.resolveDisplayNames(extractUid(oAuth2User), request);
    }

    private String extractUid(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");
        }

        String uid = oAuth2User.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");
        }

        return uid;
    }
}