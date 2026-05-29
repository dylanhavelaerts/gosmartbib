package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.SchoolNotApprovedException;
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

/**
 * Success handler voor een succesvolle Smartschool OAuth2-login.
 *
 * Na authenticatie synchroniseert deze handler de Smartschoolgebruiker met de
 * lokale database, maakt of hergebruikt hij de HTTP-sessie, plaatst hij een
 * HttpOnly marker-cookie en verrijkt hij de Spring Security authorities met de
 * actuele rol uit de database.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // pulls http://localhost:3000 lokaal, of https://gosmartbib.tech in productie
    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    private final UserService userService;

    /**
     * Verwerkt een succesvolle OAuth2-login.
     *
     * De OAuth2User wordt gesynchroniseerd naar een lokale gebruiker. Daarna wordt
     * de sessie gemarkeerd als authenticated, wordt een HttpOnly
     * AUTHENTICATED-cookie
     * geplaatst en wordt de authenticatie verrijkt met de actuele applicatierol.
     * Tot slot wordt de gebruiker teruggestuurd naar de frontend.
     *
     * Wanneer de school nog niet goedgekeurd is, wordt de gebruiker naar de
     * loginpagina gestuurd met error=school_not_approved.
     *
     * @param request        huidig HTTP request
     * @param response       huidig HTTP response
     * @param authentication succesvolle OAuth2-authenticatie van Spring Security
     * @throws IOException      wanneer de redirect niet verzonden kan worden
     * @throws ServletException wanneer de security handler faalt
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
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

            OAuth2AuthenticationToken enriched = new OAuth2AuthenticationToken(
                    oauth2User,
                    authorities,
                    ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId());

            SecurityContextHolder.getContext().setAuthentication(enriched);
            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "/";
            String targetUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (SchoolNotApprovedException ex) {
            log.warn("Login geblokkeerd: school nog niet goedgekeurd - {}", ex.getMessage());
            response.sendRedirect(frontendUrl + "/login?error=school_not_approved");
        } catch (Exception ex) {
            log.error("OAuth2 success handling failed", ex);
            response.sendRedirect(frontendUrl + "/login?error=true");
        }
    }
}
