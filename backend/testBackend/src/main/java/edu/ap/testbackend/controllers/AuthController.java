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
     * Redirects the user to Smartschool's login page via Spring Security's
     * built-in OAuth2 authorization endpoint. Scope is picked up automatically
     * from application.properties (spring.security.oauth2.client.registration.smartschool.scope=userinfo)
     */
    @GetMapping("/login")
    public RedirectView login() {
        return new RedirectView("/api/oauth2/authorization/smartschool");
    }

    /**
     * Returns the current authenticated user's safe, anonymized info.
     * Uses userID (an anonymized identifier from Smartschool) — no personal
     * data stored, safe for use with minors.
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