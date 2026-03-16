package edu.ap.testbackend.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * /login endpoint start het OAuth2 login proces, we redirecten de gebruiker naar de Smartschool login pagina.
     * Na een succesvolle login bij Smartschool zal Spring Security automatisch de gebruiker terug redirecten
     * naar de frontend (http://localhost:3000/) of (https://gosmartbib.tech/)
     */
    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationPath = request.getContextPath() + "/oauth2/authorization/smartschool";
        response.sendRedirect(authorizationPath);
    }

    /**
     * /me endpoint geeft informatie terug over de huidige ingelogde gebruiker,
     * deze informatie wordt verkregen uit de OAuth2User die Spring Security aanmaakt na een succesvolle login bij Smartschool.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // We returnen de naam, email en alle attributes van de gebruiker die we van Smartschool ontvangen
        return ResponseEntity.ok(Map.of(
                "name", user.getAttribute("name"),
                "email", user.getAttribute("email"),
                "attributes", user.getAttributes()
        ));
    }
}
