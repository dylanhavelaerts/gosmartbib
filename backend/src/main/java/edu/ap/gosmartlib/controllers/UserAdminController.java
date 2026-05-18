package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.dto.UpdateUserRoleRequest;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.users.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public Page<AdminUserDTO> listUsers(@AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String name,
            Pageable pageable) {
        return userAdminService.listUsersForAdmin(authHelper.extractUid(principal), schoolId, name, pageable);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public AdminUserDTO updateRole(
            @PathVariable long id,
            @RequestParam(required = false) Long schoolId,
            @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        return userAdminService.updateUserRole(authHelper.extractUid(principal), schoolId, id, request.role());
    }
}
