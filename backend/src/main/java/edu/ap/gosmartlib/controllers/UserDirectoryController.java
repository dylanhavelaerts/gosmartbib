package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.services.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserDirectoryService userDirectoryService;
    private final UserService userService;
    private final AuthHelper authHelper;

    @PostMapping("/display-names")
    @PreAuthorize("isAuthenticated()")
    public ResolveDisplayNamesResponse resolveDisplayNames(
            @RequestBody ResolveDisplayNamesRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        return userDirectoryService.resolveDisplayNames(authHelper.extractUid(principal), request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        userService.logUserOut(response);
        return ResponseEntity.ok().build();
    }
}
