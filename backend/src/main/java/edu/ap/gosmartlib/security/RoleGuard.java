package edu.ap.gosmartlib.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

/**
 * RoleGuard voor databasegebonden autorisatiechecks.
 *
 * Deze component wordt gebruikt in @PreAuthorize-expressies. In plaats van
 * alleen
 * te vertrouwen op frontend state of oude authorities, wordt de actuele rol van
 * de gebruiker opgehaald uit de database op basis van de Smartschool userID.
 */

@Component("roleGuard")
@RequiredArgsConstructor
public class RoleGuard {

    private final UserRepository userRepository;

    /**
     * Controleert of de huidige principal een platformadministrator is.
     *
     * Admins gebruiken een aparte loginflow met AdminPrincipal en zijn niet
     * gekoppeld
     * aan een Smartschoolschool.
     *
     * @param authentication huidige authenticatie
     * @return true wanneer de gebruiker een AdminPrincipal heeft
     */

    @Transactional(readOnly = true)
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AdminPrincipal;
    }

    /**
     * Controleert of de ingelogde Smartschoolgebruiker bibliotheekbeheerder is.
     *
     * @param authentication huidige authenticatie
     * @return true wanneer de database de rol BIBLIOTHEEKBEHEERDER bevat
     */

    @Transactional(readOnly = true)
    public boolean isBibbeheerder(Authentication authentication) {
        return hasAnyRole(authentication, UserRoles.BIBLIOTHEEKBEHEERDER);
    }

    @Transactional(readOnly = true)
    public boolean isTeacherOrBibbeheerder(Authentication authentication) {
        return hasAnyRole(authentication, UserRoles.TEACHER, UserRoles.BIBLIOTHEEKBEHEERDER);
    }

    /**
     * Algemene rolcontrole op basis van Smartschool userID.
     *
     * De userID wordt uit de OAuth2User gehaald en daarna wordt de lokale gebruiker
     * opgezocht. Daardoor blijven backendchecks correct wanneer rollen lokaal
     * worden
     * aangepast.
     *
     * @param authentication huidige authenticatie
     * @param allowedRoles   toegelaten applicatierollen
     * @return true wanneer de gebruiker één van de toegelaten rollen heeft
     */

    private boolean hasAnyRole(Authentication authentication, UserRoles... allowedRoles) {
        if (authentication == null)
            return false;
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User))
            return false;

        String uid = oAuth2User.getAttribute("userID");
        if (uid == null || uid.isBlank())
            return false;

        return userRepository.findBySmartschoolUid(uid)
                .map(UserEntity::getRole)
                .map(role -> {
                    for (UserRoles allowed : allowedRoles) {
                        if (role == allowed)
                            return true;
                    }
                    return false;
                })
                .orElse(false);
    }

}