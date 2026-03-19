package edu.ap.testbackend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

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
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of(
                "userID", user.getAttribute("userID"),
                "platform", user.getAttribute("platform"),
                "isMainAccount", user.getAttribute("isMainAccount")
        ));
    }
}