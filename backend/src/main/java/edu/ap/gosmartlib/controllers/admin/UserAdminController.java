package edu.ap.gosmartlib.controllers.admin;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequestDTO;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public Page<AdminUserDTO> listUsers(Authentication authentication,
                                        @RequestParam(required = false) Long schoolId,
                                        @RequestParam(required = false) String name,
                                        Pageable pageable) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return userAdminService.listUsersForPlatformAdmin(schoolId, name, pageable);
        return userAdminService.listUsersForLibrarian(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId, name, pageable);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public AdminUserDTO updateRole(@PathVariable long id,
                                   @RequestParam(required = false) Long schoolId,
                                   @RequestBody UpdateUserRoleRequestDTO request,
                                   Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return userAdminService.updateUserRoleForPlatformAdmin(schoolId, id, request.role());
        return userAdminService.updateUserRoleForLibrarian(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId, id, request.role());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public void deleteUser(@PathVariable long id,
                           @RequestParam(required = false) Long schoolId,
                           Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal) {
            userAdminService.deleteUserForPlatformAdmin(schoolId, id);
            return;
        }
        userAdminService.deleteUserForLibrarian(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), id);
    }
}
