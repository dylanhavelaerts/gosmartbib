package edu.ap.testbackend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // pulls http://localhost:3000 lokaal, of https://gosmartbib.tech in productie
    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // haal de gebruiker op
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // log de gebruiker
        log.info("=== SMARTSCHOOL CREDENTIALS ===");
        oauth2User.getAttributes().forEach((key, value) -> {
            log.info("{}: {}", key, value);
        });
        log.info("====================================");

        // stuur door naar de homepagina
        String targetUrl = frontendUrl + "/";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
