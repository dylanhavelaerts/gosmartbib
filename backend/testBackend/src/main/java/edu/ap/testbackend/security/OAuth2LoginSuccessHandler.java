package edu.ap.testbackend.security;

import edu.ap.testbackend.entities.UserEntity;
import edu.ap.testbackend.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // pulls http://localhost:3000 lokaal, of https://gosmartbib.tech in productie
    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    private final UserService userService;

    /**
     * Deze methode wordt aangeroepen na een succesvolle OAuth2-authenticatie. 
     * Het doel van deze methode is om de details van de geauthenticeerde gebruiker te loggen (voor debuggingdoeleinden) 
     * en vervolgens de gebruiker door te sturen naar de frontend van de applicatie.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            userService.syncUser(oauth2User);

            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);

            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "/";
            String targetUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception ex) {
            log.error("OAuth2 success handling failed", ex);
            response.sendRedirect(frontendUrl + "/login?error=true");
        }
    }
}
