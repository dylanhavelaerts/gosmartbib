package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.repositories.loan.LoanRepository;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.OneRosterUtils;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserDeletionService userDeletionService;
    private final LoanRepository loanRepository;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;

    /**
     * Geeft gebruikersbeheer-resultaten terug voor een bibliotheekbeheerder.
     *
     * <p>
     * Standaard wordt gezocht op Smartschool UID. Wanneer OneRoster voor de
     * school actief is, wordt de zoekterm ook vergeleken met de live OneRoster-
     * naam van de gebruiker. Platformbeheerders gebruiken deze methode niet en
     * blijven enkel op UID zoeken.
     * </p>
     *
     * @param actorUid Smartschool UID van de ingelogde bibliotheekbeheerder
     * @param schoolId optionele school-ID; voor bibliotheekbeheerders wordt de
     *                 eigen school gebruikt
     * @param name     zoekterm voor UID of OneRoster-naam
     * @param pageable paginering voor de resultaten
     * @return gepagineerde gebruikersresultaten
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> listUsersForLibrarian(String actorUid, Long schoolId, String name, Pageable pageable) {
        UserEntity actor = getCurrentLibrarian(actorUid);
        Long effectiveSchoolId = resolveSchoolId(actor, schoolId);
        String normalizedQuery = readString(name).toLowerCase(Locale.ROOT);

        if (normalizedQuery.isBlank()) {
            return userRepository.findBySchoolIdAndName(effectiveSchoolId, null, pageable)
                    .map(AdminUserDTO::from);
        }

        Optional<SchoolIntegrationEntity> integrationOptional = schoolIntegrationRepository
                .findBySchool_Id(effectiveSchoolId);
        if (integrationOptional.isEmpty() || !integrationOptional.get().isOnerosterEnabled()) {
            return userRepository.findBySchoolIdAndName(effectiveSchoolId, name, pageable)
                    .map(AdminUserDTO::from);
        }

        try {
            List<UserEntity> knownUsers = userRepository
                    .findAllBySchool_IdOrderBySmartschoolUidAsc(effectiveSchoolId)
                    .stream()
                    .filter(user -> user.getRole() != UserRoles.ADMIN)
                    .toList();

            Map<String, String> displayNamesByNormalizedUid = resolveLiveDisplayNames(
                    integrationOptional.get(),
                    knownUsers);

            List<UserEntity> filteredUsers = knownUsers.stream()
                    .filter(user -> matchesUidOrOneRosterName(user, displayNamesByNormalizedUid, normalizedQuery))
                    .sorted(Comparator.comparing(user -> readString(user.getSmartschoolUid()).toLowerCase(Locale.ROOT)))
                    .toList();

            return toPage(filteredUsers, pageable).map(AdminUserDTO::from);
        } catch (Exception ignored) {
            return userRepository.findBySchoolIdAndName(effectiveSchoolId, name, pageable)
                    .map(AdminUserDTO::from);
        }
    }

    @Transactional
    public AdminUserDTO updateUserRoleForLibrarian(String actorUid, Long schoolId, Long targerUserId,
            UserRoles newRole) {
        if (newRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        }

        if (newRole == UserRoles.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Gebruikers mogen niet naar ADMIN worden aangepast");
        }

        UserEntity actor = getCurrentLibrarian(actorUid);
        Long effectiveSchoolId = resolveSchoolId(actor, schoolId);

        UserEntity target = userRepository.findByIdAndSchool_Id(targerUserId, effectiveSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (target.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Je kan je eigen rol niet aanpassen");
        }

        target.setRole(newRole);

        return AdminUserDTO.from(userRepository.save(target));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> listUsersForPlatformAdmin(Long schoolId, String name, Pageable pageable) {
        if (schoolId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        return userRepository.findBySchoolIdAndName(schoolId, name, pageable).map(AdminUserDTO::from);
    }

    @Transactional
    public AdminUserDTO updateUserRoleForPlatformAdmin(Long schoolId, Long targetUserId, UserRoles newRole) {
        if (newRole == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        if (newRole == UserRoles.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ongeldige rol");
        if (schoolId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        target.setRole(newRole);
        return AdminUserDTO.from(userRepository.save(target));
    }

    @Transactional
    public void deleteUserForLibrarian(String actorUid, Long targetUserId) {
        UserEntity actor = getCurrentLibrarian(actorUid);

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, actor.getSchool().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (target.getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Je kan je eigen account niet verwijderen");

        if (loanRepository.existsBySmartschoolUserId(target.getSmartschoolUid()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gebruiker heeft nog actieve leningen");

        userDeletionService.deleteUser(target);
    }

    @Transactional
    public void deleteUserForPlatformAdmin(Long schoolId, Long targetUserId) {
        if (schoolId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (loanRepository.existsBySmartschoolUserId(target.getSmartschoolUid()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gebruiker heeft nog actieve leningen");

        userDeletionService.deleteUser(target);
    }

    private Long resolveSchoolId(UserEntity actor, Long schoolId) {
        return actor.getSchool().getId();
    }

    protected UserEntity getCurrentLibrarian(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));
        if (actor.getRole() != UserRoles.LIBRARIAN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        return actor;
    }

    /**
     * Haalt live OneRoster-namen op en koppelt ze aan lokale gebruikers.
     *
     * <p>
     * De opgehaalde OneRoster-data wordt alleen gebruikt voor zoekfunctionaliteit
     * en wordt hier niet opgeslagen in de databank.
     * </p>
     *
     * @param integration de actieve OneRoster-integratie van de school
     * @param knownUsers  lokale gebruikers van dezelfde school
     * @return map met genormaliseerde Smartschool UID als key en
     *         OneRoster-weergavenaam als value
     */
    private Map<String, String> resolveLiveDisplayNames(
            SchoolIntegrationEntity integration,
            List<UserEntity> knownUsers) {
        LinkedHashMap<String, UserEntity> usersByNormalizedUid = new LinkedHashMap<>();
        LinkedHashMap<String, UserEntity> usersBySourcedId = new LinkedHashMap<>();

        for (UserEntity user : knownUsers) {
            usersByNormalizedUid.put(normalizeUid(user.getSmartschoolUid()), user);

            String sourcedId = readString(user.getOnerosterSourcedId());
            if (!sourcedId.isBlank()) {
                usersBySourcedId.put(sourcedId, user);
            }
        }

        String accessToken = authService.getAccessToken(integration);
        List<Map<String, Object>> liveUsers = oneRosterClient.getUsers(integration, accessToken);
        LinkedHashMap<String, String> displayNamesByNormalizedUid = new LinkedHashMap<>();

        for (Map<String, Object> liveUser : liveUsers) {
            String displayName = buildDisplayName(liveUser);
            if (displayName.isBlank()) {
                continue;
            }

            for (String candidateUid : extractCandidateUids(liveUser)) {
                UserEntity knownUser = usersByNormalizedUid.get(normalizeUid(candidateUid));
                if (knownUser != null) {
                    displayNamesByNormalizedUid.putIfAbsent(
                            normalizeUid(knownUser.getSmartschoolUid()),
                            displayName);
                }
            }

            String liveSourcedId = readString(liveUser.get("sourcedId"));
            UserEntity knownUser = usersBySourcedId.get(liveSourcedId);
            if (knownUser != null) {
                displayNamesByNormalizedUid.putIfAbsent(
                        normalizeUid(knownUser.getSmartschoolUid()),
                        displayName);
            }
        }

        return displayNamesByNormalizedUid;
    }

    private boolean matchesUidOrOneRosterName(
            UserEntity user,
            Map<String, String> displayNamesByNormalizedUid,
            String normalizedQuery) {
        String normalizedUid = normalizeUid(user.getSmartschoolUid());
        String normalizedDisplayName = readString(displayNamesByNormalizedUid.get(normalizedUid))
                .toLowerCase(Locale.ROOT);

        return normalizedUid.contains(normalizedQuery)
                || normalizedDisplayName.contains(normalizedQuery);
    }

    private Page<UserEntity> toPage(List<UserEntity> users, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(users);
        }

        int start = (int) pageable.getOffset();
        if (start >= users.size()) {
            return new PageImpl<>(List.of(), pageable, users.size());
        }

        int end = Math.min(start + pageable.getPageSize(), users.size());
        return new PageImpl<>(users.subList(start, end), pageable, users.size());
    }

    private List<String> extractCandidateUids(Map<String, Object> liveUser) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        addIfPresent(candidates, OneRosterUtils.extractSmartschoolUid(liveUser));
        addIfPresent(candidates, liveUser.get("identifier"));
        addIfPresent(candidates, liveUser.get("username"));
        addIfPresent(candidates, liveUser.get("sourcedId"));

        return new ArrayList<>(candidates);
    }

    private String buildDisplayName(Map<String, Object> liveUser) {
        String givenName = readString(liveUser.get("givenName"));
        String familyName = readString(liveUser.get("familyName"));

        String fullName = (givenName + " " + familyName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }

        String fullNameField = readString(liveUser.get("fullname"));
        if (!fullNameField.isBlank()) {
            return fullNameField;
        }

        String nameField = readString(liveUser.get("name"));
        if (!nameField.isBlank()) {
            return nameField;
        }

        String username = readString(liveUser.get("username"));
        if (!username.isBlank()) {
            return username;
        }

        String identifier = readString(liveUser.get("identifier"));
        if (!identifier.isBlank()) {
            return identifier;
        }

        return "";
    }

    private void addIfPresent(Set<String> target, Object value) {
        String str = readString(value);
        if (!str.isBlank()) {
            target.add(str);
        }
    }

    private String readString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String normalizeUid(String uid) {
        return uid == null ? "" : uid.trim().toLowerCase(Locale.ROOT);
    }
}