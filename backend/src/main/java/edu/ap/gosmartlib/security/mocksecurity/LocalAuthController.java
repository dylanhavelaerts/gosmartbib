package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import edu.ap.gosmartlib.services.users.UserService;
import lombok.RequiredArgsConstructor;

import java.util.*;

@ConditionalOnProperty(name = "app.mock-role.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LocalAuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/mock-role/{role}")
    public ResponseEntity<String> switchRole(@PathVariable String role,
            HttpServletRequest request,
            HttpServletResponse response) {
        Map<String, Object> attributes = buildMockAttributes(role);

        if (attributes == null) {
            return ResponseEntity.badRequest().body("De gegeven rol is niet bekend: " + role);
        }

        OAuth2User mockUser = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "userID");

        String basisrol = (String) attributes.get("basisrol");
        UserRoles userRole = UserRoles.fromSmartschool(basisrol);
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + userRole.name()));

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                mockUser,
                authorities,
                "smartschool");
        userService.syncUser(mockUser);

        // Clear de bestaande sessie voor een nieuwe gebruiker te mocken
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        request.getSession(true);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Slaag de security contet in de sessie zodat het blijft na een reload
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        Cookie authenticatedCookie = new Cookie("AUTHENTICATED", "true");
        authenticatedCookie.setPath("/");
        authenticatedCookie.setHttpOnly(true);
        authenticatedCookie.setSecure(request.isSecure());
        response.addCookie(authenticatedCookie);

        userService.syncUser(mockUser);
        schoolRepository.findByDomain("https://aphogeschool.smartschool.be")
                .ifPresent(school -> {
                    school.setAdminApproved(true);
                    schoolRepository.save(school);
                });

        if (role.equalsIgnoreCase("librarian")) {
            userRepository.findBySmartschoolUid("mock-librarian-local")
                    .ifPresent(user -> {
                        user.setRole(UserRoles.LIBRARIAN);
                        userRepository.save(user);
                    });
        }
        if (role.equalsIgnoreCase("admin")) {
            userRepository.findBySmartschoolUid("mock-admin-local")
                    .ifPresent(user -> {
                        user.setRole(UserRoles.ADMIN);
                        userRepository.save(user);
                    });
        }

        return ResponseEntity.ok("Switched to role: " + role);
    }

    private Map<String, Object> buildMockAttributes(String role) {
        return switch (role.toLowerCase()) {
            case "leerling" -> {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("userID", "mock-student-local");
                attrs.put("name", "Jan");
                attrs.put("surname", "Janssen");
                attrs.put("fullname", "Janssen Jan");
                attrs.put("username", "janssej");
                attrs.put("email", "jan.janssen@student.ap.be");
                attrs.put("basisrol", "Leerling");
                attrs.put("status", "actief");
                attrs.put("platform", "https://aphogeschool.smartschool.be");
                attrs.put("isMainAccount", 1);
                attrs.put("isCoAccount", 0);
                attrs.put("groups", List.of(
                        Map.of("groupID", "mock-group-1", "name", "2ITSOF2", "description",
                                "Bachelor IT software - 2")));
                attrs.put("parentGroups", List.of(
                        Map.of("groupID", "mock-parent-1", "name", "2de jaars", "description", "2de jaars")));
                yield attrs;
            }
            case "leerkracht" -> {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("userID", "mock-teacher-local");
                attrs.put("name", "Petra");
                attrs.put("surname", "Peeters");
                attrs.put("fullname", "Peeters Petra");
                attrs.put("username", "peetersP");
                attrs.put("email", "petra.peeters@ap.be");
                attrs.put("basisrol", "Leerkracht");
                attrs.put("status", "actief");
                attrs.put("platform", "https://aphogeschool.smartschool.be");
                attrs.put("isMainAccount", 1);
                attrs.put("isCoAccount", 0);
                attrs.put("groups", List.of(
                        Map.of("groupID", "mock-group-1", "name", "2ITSOF2", "description", "Bachelor IT software - 2"),
                        Map.of("groupID", "mock-group-2", "name", "2ITSOF1", "description",
                                "Bachelor IT software - 1")));
                attrs.put("parentGroups", List.of());
                yield attrs;
            }
            case "librarian" -> {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("userID", "mock-librarian-local");
                attrs.put("name", "Lieve");
                attrs.put("surname", "Lemmens");
                attrs.put("fullname", "Lemmens Lieve");
                attrs.put("username", "lemmensL");
                attrs.put("email", "lieve.lemmens@ap.be");
                attrs.put("basisrol", "librarian");
                attrs.put("status", "actief");
                attrs.put("platform", "https://aphogeschool.smartschool.be");
                attrs.put("isMainAccount", 1);
                attrs.put("isCoAccount", 0);
                attrs.put("groups", List.of());
                attrs.put("parentGroups", List.of());
                yield attrs;
            }
            default -> null;
        };
    }
}