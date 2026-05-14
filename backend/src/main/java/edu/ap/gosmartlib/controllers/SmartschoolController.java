package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/smartschool")
@RequiredArgsConstructor
public class SmartschoolController {

    private final UserDirectoryService userDirectoryService;
    private final AuthHelper authHelper;

    @GetMapping("/users")
    public ResponseEntity<List<SmartschoolUserDTO>> searchSmartschoolUsers(
            @RequestParam String query,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(userDirectoryService.searchUsersForLoan(authHelper.extractUid(principal), query));
    }
}
