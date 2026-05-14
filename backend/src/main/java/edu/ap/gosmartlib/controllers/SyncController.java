package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.oneRoster.OneRosterSyncService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final OneRosterSyncService syncService;
    private final UserRepository userRepository;

    @PostMapping
    public SyncSummaryDTO syncAll(@AuthenticationPrincipal OAuth2User oauth2User) {
        String uid = oauth2User.getAttribute("userID");

        UserEntity actor = userRepository.findBySmartschoolUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (actor.getRole() != UserRoles.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        }

        return syncService.syncAll();
    }
}
