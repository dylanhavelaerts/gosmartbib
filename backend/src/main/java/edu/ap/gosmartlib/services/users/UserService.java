package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.exceptions.SchoolNotApprovedException;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.school.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service voor gebruikerssynchronisatie, sessiegegevens en logout.
 *
 * Deze service vertaalt Smartschool OAuth2-attributen naar lokale GoSmartLib
 * entiteiten zoals UserEntity, SchoolEntity en SchoolClassEntity. De service is
 * verantwoordelijk voor het aanmaken of bijwerken van gebruikers na login.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final HttpServletRequest request;
    private final SchoolClassHelper schoolClassHelper;

    /**
     * Synchroniseert een Smartschool OAuth2User met de lokale database.
     *
     * Bij elke login worden school, gebruiker, klassen en OneRoster sourcedId
     * bijgewerkt. Nieuwe scholen worden automatisch aangemaakt maar niet
     * automatisch
     * goedgekeurd. Nieuwe gebruikers van een niet-goedgekeurde school worden
     * geblokkeerd.
     *
     * De rol van bestaande gebruikers wordt bewust niet overschreven. Zo kan een
     * lokaal toegekende rol, zoals BIBLIOTHEEKBEHEERDER, behouden blijven na een
     * nieuwe Smartschool-login.
     *
     * @param oauth2User de door Smartschool aangemelde gebruiker
     * @return de aangemaakte of bijgewerkte lokale gebruiker
     * @throws SchoolNotApprovedException wanneer een nieuwe gebruiker via een
     *                                    niet-goedgekeurde school probeert in te
     *                                    loggen
     */

    @Transactional
    public UserEntity syncUser(OAuth2User oauth2User) {
        String uid = oauth2User.getAttribute("userID");
        String role = oauth2User.getAttribute("basisrol");
        String rawDomain = oauth2User.getAttribute("platform");
        String domain = rawDomain != null ? rawDomain.trim().toLowerCase().replaceAll("/+$", "") : "";

        // Zoek een school op basis van domein, als de school niet bestaat maak een
        // nieuwe aan
        SchoolEntity school = schoolRepository.findByDomain(domain)
                .orElseGet(() -> {
                    log.info("Nieuwe school gevonden, toevoegen aan database: {}", domain);
                    SchoolEntity e = new SchoolEntity();
                    e.setDomain(domain);
                    e.setName(domain); // TODO: misschien later aanpassen naar echte naam van school
                    return schoolRepository.save(e);
                });

        // Zoek een gebruiker, als gebruiker niet bestaat -> maak aan
        // Nieuwe gebruikers van een niet-goedgekeurde school worden geblokkeerd
        UserEntity user = userRepository.findBySmartschoolUid(uid)
                .orElseGet(() -> {
                    if (!school.isAdminApproved()) {
                        throw new SchoolNotApprovedException(domain);
                    }
                    log.info("Nieuwe gebruiker gevonden, toevoegen aan database: {} ({} - {})", uid, role, domain);
                    UserEntity u = new UserEntity();
                    u.setSmartschoolUid(uid);
                    u.setSchool(school);
                    u.setRole(UserRoles.fromSmartschool(role));
                    return u;
                });

        // Sync classes
        List<Map<String, Object>> groups = oauth2User.getAttribute("groups");
        List<Map<String, Object>> parentGroups = oauth2User.getAttribute("parentGroups");

        Set<SchoolClassEntity> updatedClasses = resolveClasses(
                groups != null ? groups : Collections.emptyList(),
                parentGroups != null ? parentGroups : Collections.emptyList(),
                school);

        if (!updatedClasses.equals(user.getClasses())) {
            log.info("Klassen voor gebruiker {} zijn gewijzigd. Oude klassen: {}, Nieuwe klassen: {}", uid,
                    user.getClasses(), updatedClasses);
            user.setClasses(updatedClasses);
        }
        String oneRosterSourcedId = oauth2User.getAttribute("mainAccountReferenceID");
        if (oneRosterSourcedId != null && !oneRosterSourcedId.isBlank()
                && !oneRosterSourcedId.equals(user.getOnerosterSourcedId())) {
            user.setOnerosterSourcedId(oneRosterSourcedId);
        }

        return userRepository.save(user);
    }

    public UserRoles getRoleBySmartschoolUid(String smartschoolUid) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .map(UserEntity::getRole)
                .orElseGet(() -> {
                    log.warn("No user found for smartschoolUid: {}. Defaulting to STUDENT.", smartschoolUid);
                    return UserRoles.STUDENT;
                });
    }

    /**
     * Haalt de huidige gebruiker op als UserDTO.
     *
     * Dit wordt gebruikt door /auth/me. De frontend gebruikt deze DTO om navigatie,
     * UI en client-side routebescherming op rol te sturen.
     *
     * @param uid Smartschool userID van de ingelogde gebruiker
     * @return UserDTO met rol, school en klasgegevens
     */

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String uid) {
        UserEntity user = userRepository.findDetailedBySmartschoolUid(uid)
                .orElseThrow(() -> new RuntimeException("Gebruiker niet gevonden"));

        return UserDTO.from(user);
    }

    /**
     * Logt de huidige gebruiker uit.
     *
     * De Spring Security context wordt gewist en zowel JSESSIONID als AUTHENTICATED
     * worden verwijderd, zodat de browser niet langer aan een actieve sessie
     * gekoppeld is.
     *
     * @param response HTTP response waarin de verwijdercookies worden geplaatst
     */

    public void logUserOut(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        clearCookie("JSESSIONID", response);
        clearCookie("AUTHENTICATED", response);
    }

    private void clearCookie(String name, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    // Maakt van Smartschool groups een SchoolClassEntity
    private Set<SchoolClassEntity> resolveClasses(List<Map<String, Object>> groups,
            List<Map<String, Object>> parentGroups, SchoolEntity school) {
        String grade = parentGroups.stream()
                .map(pg -> (String) pg.get("name"))
                .filter(n -> n != null && n.toLowerCase().contains("jaars"))
                .findFirst()
                .orElse(null);

        String schoolYear = resolveCurrentSchoolYear();

        Set<SchoolClassEntity> resolved = new HashSet<>();
        for (Map<String, Object> group : groups) {
            String groupId = (String) group.get("groupID");
            String name = (String) group.get("name");

            SchoolClassEntity schoolClass = schoolClassHelper.findOrCreate(school, groupId, name, schoolYear, grade);

            schoolClass.setSchoolYear(schoolYear);

            if (name != null)
                schoolClass.setName(name);
            if (grade != null)
                schoolClass.setGrade(grade);

            resolved.add(schoolClass);
        }
        return resolved;
    }

    /**
     * Zet Smartschool groups en parentGroups om naar lokale
     * SchoolClassEntity-koppelingen.
     *
     * De groups bevatten de concrete klassen van de gebruiker. De parentGroups
     * worden
     * gebruikt om extra context zoals het jaar of de graad af te leiden. Bestaande
     * klassen worden hergebruikt, nieuwe klassen worden aangemaakt via
     * SchoolClassHelper.
     *
     * @param groups       Smartschoolgroepen van de gebruiker
     * @param parentGroups bovenliggende Smartschoolgroepen
     * @param school       lokale school waartoe de gebruiker behoort
     * @return set met lokale klassen voor de gebruiker
     */

    private String resolveCurrentSchoolYear() {
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return year + "-" + (year + 1);
    }
}
