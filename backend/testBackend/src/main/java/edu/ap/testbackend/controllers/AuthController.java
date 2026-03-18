package edu.ap.testbackend.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OAuth2AuthorizationRequestResolver authorizationRequestResolver;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    public AuthController(
        OAuth2AuthorizationRequestResolver pkceDisabledResolver,
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
        this.authorizationRequestResolver = pkceDisabledResolver;
    this.authorizationRequestRepository = authorizationRequestRepository;
    }

    /**
     * /login endpoint start het OAuth2 login proces, we redirecten de gebruiker naar de Smartschool login pagina.
     * Na een succesvolle login bij Smartschool zal Spring Security automatisch de gebruiker terug redirecten
     * naar de frontend (http://localhost:3000/) of (https://gosmartbib.tech/)
     */
//    @GetMapping("/login")
//    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String authorizationPath = request.getContextPath() + "/oauth2/authorization/smartschool";
//        response.sendRedirect(authorizationPath);
//    }

    // Bouwt de provider URL met alle OAuth query params (o.a. client_id, redirect_uri, response_type, scope en state).
    // aan de hand van de properties
    @GetMapping("/login")
    public RedirectView login(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest =
                authorizationRequestResolver.resolve(request, "smartschool");

        if (authorizationRequest == null) {
            return new RedirectView("/login?error");
        }

        authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response);
        return new RedirectView(authorizationRequest.getAuthorizationRequestUri());
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

        return ResponseEntity.ok(Map.of(
                "name", user.getAttribute("name"),
                "email", user.getAttribute("email"),
                "attributes", user.getAttributes()
        ));
    }
}
