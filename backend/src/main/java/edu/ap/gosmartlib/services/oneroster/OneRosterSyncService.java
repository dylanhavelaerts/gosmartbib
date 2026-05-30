package edu.ap.gosmartlib.services.oneroster;

import edu.ap.gosmartlib.dto.sync.SchoolSyncResultDTO;
import edu.ap.gosmartlib.dto.sync.SyncResultDTO;
import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.school.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.school.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolintegration.SmartschoolOneRosterClient;
import edu.ap.gosmartlib.services.users.UserDeletionService;
import edu.ap.gosmartlib.util.OneRosterUtils;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service die de volledige synchronisatie met Smartschool OneRoster uitvoert.
 *
 * <p>
 * De service haalt gebruikers, klassen en inschrijvingen op via de
 * OneRoster API en verwerkt deze gegevens in de lokale databank. Nieuwe
 * gebruikers worden aangemaakt, ontbrekende gebruikers worden verwijderd via
 * {@link UserDeletionService}, klassen worden bijgewerkt en inschrijvingen
 * worden opnieuw gekoppeld.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneRosterSyncService {

    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient client;
    private final UserRepository userRepository;
    private final UserDeletionService userDeletionService;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SchoolClassRepository schoolClassRepository;

    /**
     * Synchroniseert alle scholen waarvoor OneRoster is ingeschakeld.
     *
     * <p>
     * Voor elke actieve schoolintegratie wordt
     * {@link #syncSchool(SchoolIntegrationEntity)}
     * uitgevoerd. De resultaten worden samengevoegd tot één globale samenvatting.
     * </p>
     *
     * @return een samenvatting met totalen en resultaten per school
     */
    @Transactional
    public SyncSummaryDTO syncAll() {
        List<SchoolIntegrationEntity> integrations = schoolIntegrationRepository.findAllByOnerosterEnabledTrue();

        List<SchoolSyncResultDTO> results = new ArrayList<>();
        int totalAdded = 0;
        int totalRemoved = 0;

        for (SchoolIntegrationEntity integration : integrations) {
            SyncResultDTO result = syncSchool(integration);
            results.add(new SchoolSyncResultDTO(
                    integration.getSchool().getDomain(),
                    result.added(),
                    result.removed(),
                    result.classesSynced(),
                    result.enrollmentsSynced(),
                    result.errors()));
            totalAdded += result.added();
            totalRemoved += result.removed();
        }

        log.info("Full sync completed: {} added, {} removed across {} schools",
                totalAdded, totalRemoved, integrations.size());

        return new SyncSummaryDTO(totalAdded, totalRemoved, results);
    }

    /**
     * Synchroniseert één school met Smartschool OneRoster.
     *
     * <p>
     * De methode voert de volgende stappen uit:
     * </p>
     * <ol>
     * <li>Een access token ophalen via de client credentials flow.</li>
     * <li>OneRoster-gebruikers ophalen en koppelen op basis van Smartschool
     * UID.</li>
     * <li>Nieuwe gebruikers aanmaken.</li>
     * <li>Gebruikers verwijderen die niet meer in OneRoster voorkomen.</li>
     * <li>Klassen ophalen of bijwerken.</li>
     * <li>Inschrijvingen ophalen en gebruikers opnieuw aan klassen koppelen.</li>
     * <li>De laatste synchronisatiedatum en eventuele fouten opslaan.</li>
     * </ol>
     *
     * <p>
     * Fouten binnen deelstappen worden verzameld zodat de sync niet volledig
     * hoeft te stoppen wanneer bijvoorbeeld alleen de klassensync mislukt.
     * </p>
     *
     * @param integration de schoolintegratie waarvoor de sync uitgevoerd wordt
     * @return het resultaat van de synchronisatie voor deze school
     */
    @Transactional
    public SyncResultDTO syncSchool(SchoolIntegrationEntity integration) {
        log.info("Starting OneRoster sync for school {}", integration.getSchool().getDomain());

        List<String> errors = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int classesSynced = 0;
        int enrollmentsSynced = 0;

        try {
            String accessToken = authService.getAccessToken(integration);

            List<Map<String, Object>> onerosterUsers = client.getUsers(integration, accessToken);

            Map<String, Map<String, Object>> onerosterByUid = onerosterUsers.stream()
                    .filter(u -> OneRosterUtils.extractSmartschoolUid(u) != null)
                    .collect(Collectors.toMap(
                            OneRosterUtils::extractSmartschoolUid,
                            u -> u,
                            (a, b) -> a));

            List<UserEntity> dbUsers = userRepository
                    .findAllBySchool_IdOrderBySmartschoolUidAsc(integration.getSchool().getId());

            Map<String, UserEntity> dbByUid = dbUsers.stream()
                    .collect(Collectors.toMap(UserEntity::getSmartschoolUid, u -> u));

            for (Map.Entry<String, Map<String, Object>> entry : onerosterByUid.entrySet()) {
                if (!dbByUid.containsKey(entry.getKey())) {
                    try {
                        UserEntity created = createUser(entry.getValue(), integration.getSchool());
                        dbByUid.put(created.getSmartschoolUid(), created);
                        added++;
                    } catch (Exception e) {
                        log.error("Failed to create user {}: {}", entry.getKey(), e.getMessage());
                        errors.add("Maken van gebruiker mislukt voor " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            }

            for (Map.Entry<String, UserEntity> entry : dbByUid.entrySet()) {
                if (!onerosterByUid.containsKey(entry.getKey())) {
                    try {
                        userDeletionService.deleteUser(entry.getValue());
                        removed++;
                    } catch (Exception e) {
                        log.error("Failed to delete user {}: {}", entry.getKey(), e.getMessage());
                        errors.add("Verwijderen mislukt voor " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            }

            Map<String, SchoolClassEntity> classMap = new HashMap<>();
            try {
                List<Map<String, Object>> onerosterClasses = client.getClasses(integration, accessToken);
                for (Map<String, Object> c : onerosterClasses) {
                    String sourcedId = (String) c.get("sourcedId");
                    if (sourcedId == null)
                        continue;

                    SchoolClassEntity entity = schoolClassRepository
                            .findBySmartschoolGroupId(sourcedId)
                            .orElse(new SchoolClassEntity());

                    entity.setSchool(integration.getSchool());
                    entity.setSmartschoolGroupId(sourcedId);
                    entity.setName(extractTitle(c));
                    entity.setGrade(extractGrade(c));
                    schoolClassRepository.save(entity);
                    classMap.put(sourcedId, entity);
                    classesSynced++;
                }
                log.info("Classes synced for {}: {}", integration.getSchool().getDomain(), classesSynced);
            } catch (Exception e) {
                log.error("Class sync failed for {}: {}", integration.getSchool().getDomain(), e.getMessage());
                errors.add("Klassen sync mislukt: " + e.getMessage());
            }

            Map<String, UserEntity> usersByOnerosterSourcedId = dbByUid.values().stream()
                    .filter(u -> u.getOnerosterSourcedId() != null)
                    .collect(Collectors.toMap(UserEntity::getOnerosterSourcedId, u -> u, (a, b) -> a));

            for (UserEntity user : usersByOnerosterSourcedId.values()) {
                user.getClasses().clear();
            }

            try {
                List<Map<String, Object>> enrollments = client.getEnrollments(integration, accessToken);
                for (Map<String, Object> enrollment : enrollments) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userRef = (Map<String, Object>) enrollment.get("user");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> classRef = (Map<String, Object>) enrollment.get("class");
                    if (userRef == null || classRef == null)
                        continue;

                    String userSourcedId = (String) userRef.get("sourcedId");
                    String classSourcedId = (String) classRef.get("sourcedId");

                    UserEntity user = usersByOnerosterSourcedId.get(userSourcedId);
                    SchoolClassEntity schoolClass = classMap.get(classSourcedId);

                    if (user != null && schoolClass != null) {
                        user.getClasses().add(schoolClass);
                        enrollmentsSynced++;
                    }
                }
                userRepository.saveAll(usersByOnerosterSourcedId.values());
                log.info("Enrollments synced for {}: {}", integration.getSchool().getDomain(), enrollmentsSynced);
            } catch (Exception e) {
                log.error("Enrollment sync failed for {}: {}", integration.getSchool().getDomain(), e.getMessage());
                errors.add("Inschrijvingen sync mislukt: " + e.getMessage());
            }

            integration.setLastSyncAt(LocalDateTime.now());
            integration.setLastError(errors.isEmpty() ? null : String.join("; ", errors));
            schoolIntegrationRepository.save(integration);

            log.info("Sync completed for school {}: {} users added, {} removed, {} classes, {} enrollments, {} errors",
                    integration.getSchool().getDomain(), added, removed, classesSynced, enrollmentsSynced,
                    errors.size());

        } catch (Exception e) {
            log.error("Sync failed for school {}: {}", integration.getSchool().getDomain(), e.getMessage());
            integration.setLastError("Sync mislukt: " + e.getMessage());
            schoolIntegrationRepository.save(integration);
            errors.add("Sync mislukt: " + e.getMessage());
        }

        return new SyncResultDTO(added, removed, classesSynced, enrollmentsSynced, errors);
    }

    private UserEntity createUser(Map<String, Object> onerosterUser, SchoolEntity school) {
        String uid = OneRosterUtils.extractSmartschoolUid(onerosterUser);
        String sourcedId = (String) onerosterUser.get("sourcedId");
        String role = (String) onerosterUser.get("role");

        UserEntity user = userRepository.findBySmartschoolUid(uid)
                .orElseGet(UserEntity::new);

        boolean isNew = user.getId() == null;
        user.setSmartschoolUid(uid);
        user.setOnerosterSourcedId(sourcedId);
        user.setSchool(school);
        if (isNew) {
            user.setRole(UserRoles.fromOneRoster(role));
        }
        userRepository.save(user);
        log.info("{} user {} from OneRoster", isNew ? "Created" : "Updated", uid);
        return user;
    }

    private String extractTitle(Map<String, Object> c) {
        Object title = c.get("title");
        return title instanceof String s ? s : "Onbekende klas";
    }

    private String extractGrade(Map<String, Object> c) {
        Object grades = c.get("grades");
        if (grades instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        return null;
    }
}
