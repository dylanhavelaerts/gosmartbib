package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.services.users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

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
     * Het doel van deze methode is om de details van de geauthenticeerde gebruiker
     * te loggen (voor debuggingdoeleinden)
     * en vervolgens de gebruiker door te sturen naar de frontend van de applicatie.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            UserEntity user = userService.syncUser(oauth2User);

            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);

            Cookie authenticatedCookie = new Cookie("AUTHENTICATED", "true");
            authenticatedCookie.setPath("/");
            authenticatedCookie.setHttpOnly(true);
            authenticatedCookie.setSecure(request.isSecure());
            response.addCookie(authenticatedCookie);

            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            OAuth2AuthenticationToken enriched = new OAuth2AuthenticationToken(
                    oauth2User,
                    authorities,
                    ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
            );

            SecurityContextHolder.getContext().setAuthentication(enriched);
            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "/";
            String targetUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception ex) {
            log.error("OAuth2 success handling failed", ex);
            response.sendRedirect(frontendUrl + "/login?error=true");
        }
    }
}
