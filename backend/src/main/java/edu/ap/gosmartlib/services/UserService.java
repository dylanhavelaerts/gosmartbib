package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.UserDTO;
import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // Nog bespreken met klant (hoe lang voor inactive accounts verwijderd mogen
    // worden
    // private static final long DELETION_DAYS = 365; --> ook nog te implementeren

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;

    /**
     * Wordt aangeroepen elke login
     * - Als een gebruiker niet bestaat -> nieuw account aanmaken
     * - Als een gebruiker al bestaat -> gegevens updaten indien nodig (naam,
     * klassen, ...)
     * - ROL wordt niet overgeschreven aangezien een leerkracht miss de bibbeheerder
     * rol heeft gekregen
     */
    @Transactional
    public UserEntity syncUser(OAuth2User oauth2User) {
        String uid = oauth2User.getAttribute("userID");
        String role = oauth2User.getAttribute("basisrol");
        String domain = oauth2User.getAttribute("platform");

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
        UserEntity user = userRepository.findBySmartschoolUid(uid)
                .orElseGet(() -> {
                    log.info("Nieuwe gebruiker gevonden, toevoegen aan database: {} ({} - {})", uid, role, domain);
                    UserEntity u = new UserEntity();
                    u.setSmartschoolUid(uid);
                    u.setSchool(school);
                    u.setRole(UserRoles.fromSmartschool(role));
                    return u;
                });

        if (!user.isActive()) {
            user.setActive(true);
            log.info("Gebruiker {} is opnieuw actief geworden, status bijgewerkt", uid);
        }

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

        return userRepository.save(user);
    }

    //
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String uid) {
        UserEntity user = userRepository.findDetailedBySmartschoolUid(uid)
                .orElseThrow(() -> new RuntimeException("Gebruiker niet gevonden"));

        return UserDTO.from(user);
    }

    // Maakt van Smartschool groups een SchoolClassEntity
    private Set<SchoolClassEntity> resolveClasses(
            List<Map<String, Object>> groups,
            List<Map<String, Object>> parentGroups,
            SchoolEntity school) {
        String gradeLevel = parentGroups.stream()
                .map(pg -> (String) pg.get("name"))
                .filter(n -> n != null && n.toLowerCase().contains("jaars"))
                .findFirst()
                .orElse(null);

        String schoolYear = resolveCurrentSchoolYear();

        Set<SchoolClassEntity> resolved = new HashSet<>();
        for (Map<String, Object> group : groups) {
            String groupId = (String) group.get("groupID");
            String name = (String) group.get("name");

            SchoolClassEntity schoolClass = schoolClassRepository.findBySmartschoolGroupId(groupId)
                    .orElseGet(() -> {
                        SchoolClassEntity c = new SchoolClassEntity(
                                school, groupId, name, schoolYear, gradeLevel);
                        log.info("New class created: {} ({})", name, groupId);
                        return schoolClassRepository.save(c);
                    });

            resolved.add(schoolClass);
        }
        return resolved;
    }

    // Vindt het momentele schooljaar
    private String resolveCurrentSchoolYear() {
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return year + "-" + (year + 1);
    }
}
