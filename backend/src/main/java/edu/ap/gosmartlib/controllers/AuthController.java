package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import java.util.Set;

/**
 * Controller voor de login- en sessie-endpoints van de OAuthFlow.
 *
 * Deze controller start de Smartschool OAuth2-login via /auth/login en levert
 * via /auth/me de huidige ingelogde gebruiker aan de frontend. De frontend
 * gebruikt /auth/me als bron van waarheid, omdat HttpOnly sessiecookies niet
 * rechtstreeks door JavaScript gelezen kunnen worden.
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    private final UserService userService;

    /**
     * Start de Smartschool OAuth2-login.
     *
     * De frontend navigeert naar dit endpoint, waarna Spring Security de OAuth2
     * authorization request opbouwt en de browser naar Smartschool doorstuurt.
     *
     * @return redirect naar de Spring Security OAuth2 authorization endpoint
     */
    @GetMapping("/login")
    public RedirectView login() {
        return new RedirectView("/api/oauth2/authorization/smartschool");
    }

    /**
     * Geeft de huidige ingelogde gebruiker terug op basis van de actieve sessie.
     *
     * Voor gewone Smartschoolgebruikers wordt de userID uit de OAuth2User gehaald
     * en wordt de lokale UserDTO uit de database opgehaald. Voor platformadmins
     * wordt de AdminPrincipal omgezet naar een UserDTO met ADMIN-rol.
     *
     * @param authentication de huidige Spring Security authenticatie
     * @return de huidige gebruiker, of 401 wanneer er geen geldige sessie bestaat
     */
    @GetMapping("/me")
    @Transactional
    public ResponseEntity<UserDTO> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (authentication.getPrincipal() instanceof AdminPrincipal admin) {
            return ResponseEntity.ok(new UserDTO(admin.getId(), null, UserRoles.ADMIN, null, Set.of()));
        }

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String uid = oauth2User.getAttribute("userID");
            return ResponseEntity.ok(userService.getCurrentUser(uid));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}