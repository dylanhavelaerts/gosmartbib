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
    public boolean isBibbeheerder(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            return false;
        }

        String uid = oAuth2User.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            return false;
        }

        return userRepository.findBySmartschoolUid(uid)
                .map(UserEntity::getRole)
                .filter(role -> role == UserRoles.BIBLIOTHEEKBEHEERDER)
                .isPresent();
    }
}
