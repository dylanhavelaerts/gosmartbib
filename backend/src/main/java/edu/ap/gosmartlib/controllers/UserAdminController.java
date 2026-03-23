package edu.ap.gosmartlib.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequest;
import edu.ap.gosmartlib.services.UserAdminService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    // Runt dit eerst voor al de rest gebeurt (methode beveiliging)
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    // AuthenticationPrinciple injecteerd automatisch de gebruikergegevens
    public List<AdminUserDTO> listUsers(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return userAdminService.listUsersForAdmin(extractUid(oAuth2User));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public AdminUserDTO updateRole(
            @PathVariable long id,
            @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        return userAdminService.updateUserRole(extractUid(oAuth2User), id, request.role());
    }

    // Hulpmethodes
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
