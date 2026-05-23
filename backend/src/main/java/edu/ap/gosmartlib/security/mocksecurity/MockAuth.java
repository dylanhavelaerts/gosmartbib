package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MockAuth extends OncePerRequestFilter {

    private final UserService userService;
    private final SchoolRepository schoolRepository;

    /**
     * Deze klasse injecteert een basis gebruiker in je sessie in de lokale omgeving
     */


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("userID", "mock-student-local");
            attributes.put("name", "Jan");
            attributes.put("surname", "Janssen");
            attributes.put("fullname", "Janssen Jan");
            attributes.put("username", "janssej");
            attributes.put("email", "jan.janssen@student.ap.be");
            attributes.put("basisrol", "Leerling");
            attributes.put("status", "actief");
            attributes.put("platform", "https://aphogeschool.smartschool.be");
            attributes.put("isMainAccount", 1);
            attributes.put("isCoAccount", 0);
            attributes.put("groups", List.of(
                    Map.of("groupID", "mock-group-1", "name", "2ITSOF2", "description",
                            "Bachelor IT software - 2")));
            attributes.put("parentGroups", List.of(
                    Map.of("groupID", "mock-parent-1", "name", "2de jaars", "description",
                            "2de jaars")));

            OAuth2User mockUser = new DefaultOAuth2User(
                    Collections.emptyList(),
                    attributes,
                    "userID");

            String basisrol = (String) attributes.get("basisrol");
            UserRoles userRole = UserRoles.fromSmartschool(basisrol);
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + userRole.name())
            );

            OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                    mockUser,
                    authorities,
                    "smartschool");

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Goedkeuren voor syncUser loopt, anders blokkeert de gate nieuwe gebruikers
            schoolRepository.findByDomain("https://aphogeschool.smartschool.be")
                    .ifPresentOrElse(
                            school -> {
                                if (!school.isAdminApproved()) {
                                    school.setAdminApproved(true);
                                    schoolRepository.save(school);
                                }
                            },
                            () -> {
                                try {
                                    SchoolEntity newSchool = new SchoolEntity();
                                    newSchool.setDomain("https://aphogeschool.smartschool.be");
                                    newSchool.setName("AP Hogeschool (Mock)");
                                    newSchool.setAdminApproved(true);
                                    schoolRepository.save(newSchool);
                                } catch (DataIntegrityViolationException ignored) {
                                    schoolRepository.findByDomain("https://aphogeschool.smartschool.be")
                                            .ifPresent(existing -> {
                                                if (!existing.isAdminApproved()) {
                                                    existing.setAdminApproved(true);
                                                    schoolRepository.save(existing);
                                                }
                                            });
                                }
                            });

            // lokale gebruikers injecteren in de db
            userService.syncUser(mockUser);
        }

        filterChain.doFilter(request, response);
    }
}