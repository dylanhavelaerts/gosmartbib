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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    private final UserService userService;


    /**
     * Redirect de gebruiker naar de Smartschool OAuth2 loginpagina.
     * Na succesvolle login zal de gebruiker teruggestuurd worden naar de frontend,
     * waar de app de gebruikersinfo kan ophalen via de /auth/me endpoint.
     */
    @GetMapping("/login")
    public RedirectView login() {
        return new RedirectView("/api/oauth2/authorization/smartschool");
    }

    /**
     * Endpoint om de informatie van de ingelogde gebruiker op te halen.
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