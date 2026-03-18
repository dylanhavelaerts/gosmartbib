package edu.ap.testbackend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final String smartschoolAuthorizationUrl;
    private final String smartschoolClientId;
    private final String redirectUrl;

    public AuthController(
            @Value("${spring.security.oauth2.client.provider.smartschool.authorization-uri}") String smartschoolAuthorizationUrl,
            @Value("${spring.security.oauth2.client.registration.smartschool.client-id}") String smartschoolClientId,
            @Value("${spring.security.oauth2.client.registration.smartschool.redirect-uri}") String redirectUrl
    ) {
        this.smartschoolAuthorizationUrl = smartschoolAuthorizationUrl;
        this.smartschoolClientId = smartschoolClientId;
        this.redirectUrl = redirectUrl;
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

    // Voorbeeld: “/OAuth?client_id=[client_id]&redirect_uri=[redirect_uri]&response_type=code&scope=[scope1 scope2 scope3]”
    @GetMapping("/login")
    public RedirectView login() {
        String url = UriComponentsBuilder.fromUriString(smartschoolAuthorizationUrl)
                .queryParam("client_id", smartschoolClientId)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "userinfo")
                .build(true)
                .toUriString();

        return new RedirectView(url);
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
