package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    private final UserRepository userRepository;

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
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String uid = oauth2User.getAttribute("userID");
        UserEntity user = userRepository.findBySmartschoolUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserDTO.from(user));
    }
}