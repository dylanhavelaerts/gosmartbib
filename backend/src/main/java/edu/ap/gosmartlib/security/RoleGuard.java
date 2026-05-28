package edu.ap.gosmartlib.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

@Component("roleGuard")
@RequiredArgsConstructor
public class RoleGuard {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminPrincipal;
    }


    @Transactional(readOnly = true)
    public boolean isLibrarian(Authentication authentication) {
        return hasAnyRole(authentication, UserRoles.LIBRARIAN);
    }

    @Transactional(readOnly = true)
    public boolean isTeacherOrLibrarian(Authentication authentication) {
        return hasAnyRole(authentication, UserRoles.TEACHER, UserRoles.LIBRARIAN);
    }

    @Transactional(readOnly = true)
    public boolean isLibrarianOrAdmin(Authentication authentication) {
        if (isAdmin(authentication)) return true;
        return hasAnyRole(authentication, UserRoles.LIBRARIAN);
    }

    private boolean hasAnyRole(Authentication authentication, UserRoles... allowedRoles) {
        if (authentication == null) return false;
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) return false;

        String uid = oAuth2User.getAttribute("userID");
        if (uid == null || uid.isBlank()) return false;

        return userRepository.findBySmartschoolUid(uid)
                .map(UserEntity::getRole)
                .map(role -> {
                    for (UserRoles allowed : allowedRoles) {
                        if (role == allowed) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

}