package edu.ap.gosmartlib.security;

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

    /**
     * Deze methode wordt aangeroepen na een succesvolle OAuth2-authenticatie.
     * Het doel van deze methode is om de details van de geauthenticeerde gebruiker
     * te loggen (voor debuggingdoeleinden)
     * en vervolgens de gebruiker door te sturen naar de frontend van de applicatie.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            Object principal = authentication.getPrincipal();

            // Log de details van de geauthenticeerde gebruiker,
            // vooral de attributen die door Smartschool worden teruggegeven.
            if (principal instanceof OAuth2User oauth2User) {
                log.info("=== SMARTSCHOOL CREDENTIALS ===");
                oauth2User.getAttributes().forEach((key, value) -> log.info("{}: {}", key, value));
                log.info("====================================");
            } else {
                log.warn("OAuth2 login principal is not an OAuth2User: {}", principal);
            }

            // Bepaal de redirect URL na succesvolle login, standaard naar de frontend root.
            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "/";
            String targetUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception ex) {
            log.error("OAuth2 success handling failed", ex);

            // In geval van een fout tijdens het verwerken van de succesvolle authenticatie,
            // redirect de gebruiker naar een foutpagina op de frontend.
            String fallback = (frontendUrl != null && !frontendUrl.isBlank())
                    ? frontendUrl + "/login?error=true"
                    : "/login?error=true";
            response.sendRedirect(fallback);
        }
    }
}
