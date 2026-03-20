package edu.ap.testbackend.security.mocksecurity;

import edu.ap.testbackend.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MockAuth extends OncePerRequestFilter {

    private final UserService userService;

    /**
     * Deze klasse injecteert een basis gebruiker in je sessie in de lokale omgeving
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("userID",        "mock-student-local");
            attributes.put("name",          "Jan");
            attributes.put("surname",       "Janssen");
            attributes.put("fullname",      "Janssen Jan");
            attributes.put("username",      "janssej");
            attributes.put("email",         "jan.janssen@student.ap.be");
            attributes.put("basisrol",      "Leerling");
            attributes.put("status",        "actief");
            attributes.put("platform",      "https://aphogeschool.smartschool.be");
            attributes.put("isMainAccount", 1);
            attributes.put("isCoAccount",   0);
            attributes.put("groups", List.of(
                    Map.of("groupID", "mock-group-1", "name", "2ITSOF2", "description", "Bachelor IT software - 2")
            ));
            attributes.put("parentGroups", List.of(
                    Map.of("groupID", "mock-parent-1", "name", "2de jaars", "description", "2de jaars")
            ));

            OAuth2User mockUser = new DefaultOAuth2User(
                    Collections.emptyList(),
                    attributes,
                    "userID"
            );

            OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                    mockUser,
                    Collections.emptyList(),
                    "smartschool"
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // lokale gebruikers injecteren in de db
            userService.syncUser(mockUser);
        }

        filterChain.doFilter(request, response);
    }
}