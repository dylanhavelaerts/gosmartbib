package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.dto.loan.SmartschoolUserDTO;
<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
import edu.ap.gosmartlib.entities.schoolEntities.SchoolIntegrationEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.OneRosterUtils;
import edu.ap.gosmartlib.util.UserRoles;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDirectoryService {

    private static final int MAX_UIDS_PER_REQUEST = 100;

    private final UserRepository userRepository;
    private final SchoolIntegrationRepository schoolIntegrationRepository;
    private final SmartschoolOneRosterAuthService authService;
    private final SmartschoolOneRosterClient oneRosterClient;

    @Transactional(readOnly = true)
    public ResolveDisplayNamesResponse resolveDisplayNames(String actorUid, ResolveDisplayNamesRequest request) {
        if (request == null || request.uids() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UID-lijst ontbreekt");
        }

        List<String> requestedUids = normalizeRequestedUids(request.uids());

        if (requestedUids.size() > MAX_UIDS_PER_REQUEST) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Te veel UIDs in één request. Maximum is " + MAX_UIDS_PER_REQUEST);
        }

        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ingelogde gebruiker niet gevonden"));

        if (requestedUids.isEmpty()) {
            return new ResolveDisplayNamesResponse(
                    true,
                    0,
                    0,
                    Map.of(),
                    List.of(),
                    "Geen UIDs gevraagd");
        }

        Long schoolId = resolveSchoolId(actor, request.schoolId());

        List<UserEntity> knownUsers = userRepository.findAllBySchool_IdAndSmartschoolUidIn(
                schoolId,
                requestedUids);

        if (knownUsers.isEmpty()) {
            return new ResolveDisplayNamesResponse(
                    true,
                    requestedUids.size(),
                    0,
                    Map.of(),
                    requestedUids,
                    "Geen gekende gebruikers gevonden in dezelfde school");
        }

        Set<String> allowedNormalizedUids = new LinkedHashSet<>();
        for (UserEntity user : knownUsers) {
            allowedNormalizedUids.add(normalizeUid(user.getSmartschoolUid()));
        }

        LinkedHashMap<String, String> requestedUidByNormalizedUid = new LinkedHashMap<>();
        for (String requestedUid : requestedUids) {
            requestedUidByNormalizedUid.put(normalizeUid(requestedUid), requestedUid);
        }

        Optional<SchoolIntegrationEntity> integrationOptional = schoolIntegrationRepository.findBySchool_Id(schoolId);
        if (integrationOptional.isEmpty()) {
            return new ResolveDisplayNamesResponse(
                    false,
                    requestedUids.size(),
                    0,
                    Map.of(),
                    requestedUids,
                    "Geen schoolintegratie geconfigureerd");
        }

        SchoolIntegrationEntity integration = integrationOptional.get();
        if (!integration.isOnerosterEnabled()) {
            return new ResolveDisplayNamesResponse(
                    false,
                    requestedUids.size(),
                    0,
                    Map.of(),
                    requestedUids,
                    "Schoolintegratie is niet ingeschakeld");
        }

        try {
            String accessToken = authService.getAccessToken(integration);
            List<Map<String, Object>> liveUsers = oneRosterClient.getUsers(integration, accessToken);

            LinkedHashMap<String, String> resolvedDisplayNames = new LinkedHashMap<>();

            for (Map<String, Object> liveUser : liveUsers) {
                String displayName = buildDisplayName(liveUser);
                if (displayName.isBlank()) {
                    continue;
                }

                for (String candidateUid : extractCandidateUids(liveUser)) {
                    String normalizedCandidateUid = normalizeUid(candidateUid);

                    if (!allowedNormalizedUids.contains(normalizedCandidateUid)) {
                        continue;
                    }

                    String requestedUid = requestedUidByNormalizedUid.get(normalizedCandidateUid);
                    if (requestedUid != null && !resolvedDisplayNames.containsKey(requestedUid)) {
                        resolvedDisplayNames.put(requestedUid, displayName);
                    }
                }

                // Fallback: match by onerosterSourcedId when smsc.legacyIdentifier is absent
                String liveSourcedId = readString(liveUser.get("sourcedId"));
                if (!liveSourcedId.isBlank()) {
                    for (UserEntity knownUser : knownUsers) {
                        if (liveSourcedId.equals(knownUser.getOnerosterSourcedId())) {
                            String requestedUid = requestedUidByNormalizedUid.get(normalizeUid(knownUser.getSmartschoolUid()));
                            if (requestedUid != null && !resolvedDisplayNames.containsKey(requestedUid)) {
                                resolvedDisplayNames.put(requestedUid, displayName);
                            }
                            break;
                        }
                    }
                }
            }

            List<String> unresolvedUids = requestedUids.stream()
                    .filter(uid -> !resolvedDisplayNames.containsKey(uid))
                    .toList();

            return new ResolveDisplayNamesResponse(
                    true,
                    requestedUids.size(),
                    resolvedDisplayNames.size(),
                    resolvedDisplayNames,
                    unresolvedUids,
                    resolvedDisplayNames.isEmpty()
                            ? "Geen display names gevonden"
                            : "Display names succesvol opgehaald");
        } catch (Exception ex) {
            return new ResolveDisplayNamesResponse(
                    false,
                    requestedUids.size(),
                    0,
                    Map.of(),
                    requestedUids,
                    "Ophalen van display names mislukt: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SmartschoolUserDTO> searchUsersForLoan(String actorUid, String query) {
        String normalizedQuery = readString(query).toLowerCase(Locale.ROOT);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ingelogde gebruiker niet gevonden"));

        Long schoolId = actor.getSchool().getId();

        List<UserEntity> knownUsers = userRepository
                .findAllBySchool_IdOrderBySmartschoolUidAsc(schoolId);
        if (knownUsers.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, UserEntity> knownUsersByNormalizedUid = new LinkedHashMap<>();
        for (UserEntity user : knownUsers) {
            knownUsersByNormalizedUid.put(normalizeUid(user.getSmartschoolUid()), user);
        }

        LinkedHashMap<String, String> liveDisplayNamesByNormalizedUid = new LinkedHashMap<>();

        Optional<SchoolIntegrationEntity> integrationOptional = schoolIntegrationRepository.findBySchool_Id(schoolId);
        if (integrationOptional.isPresent() && integrationOptional.get().isOnerosterEnabled()) {
            try {
                SchoolIntegrationEntity integration = integrationOptional.get();
                String accessToken = authService.getAccessToken(integration);
                List<Map<String, Object>> liveUsers = oneRosterClient.getUsers(integration, accessToken);

                for (Map<String, Object> liveUser : liveUsers) {
                    String displayName = buildDisplayName(liveUser);
                    if (displayName.isBlank()) {
                        continue;
                    }

                    for (String candidateUid : extractCandidateUids(liveUser)) {
                        String normalizedCandidateUid = normalizeUid(candidateUid);

                        if (!knownUsersByNormalizedUid.containsKey(normalizedCandidateUid)) {
                            continue;
                        }

                        liveDisplayNamesByNormalizedUid.putIfAbsent(normalizedCandidateUid, displayName);
                    }

                    // Fallback: match by onerosterSourcedId when smsc.legacyIdentifier is absent
                    String liveSourcedId = readString(liveUser.get("sourcedId"));
                    if (!liveSourcedId.isBlank()) {
                        for (UserEntity knownUser : knownUsers) {
                            if (liveSourcedId.equals(knownUser.getOnerosterSourcedId())) {
                                liveDisplayNamesByNormalizedUid.putIfAbsent(
                                        normalizeUid(knownUser.getSmartschoolUid()), displayName);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Als live ophalen mislukt, vallen we terug op de gekende UID-zoeking.
            }
        }

        return knownUsers.stream()
                .map(user -> {
                    String normalizedUid = normalizeUid(user.getSmartschoolUid());

                    String displayName = liveDisplayNamesByNormalizedUid.getOrDefault(
                            normalizedUid,
                            user.getSmartschoolUid());

                    String classGroup = user.getClasses().stream()
                            .findFirst()
                            .map(SchoolClassEntity::getName)
                            .orElse("Onbekend");

                    String schoolName = user.getSchool() != null && user.getSchool().getName() != null
                            ? user.getSchool().getName()
                            : "Onbekend";

                    String schoolIdValue = user.getSchool() != null && user.getSchool().getId() != null
                            ? String.valueOf(user.getSchool().getId())
                            : "";

                    String photoUrl = "https://ui-avatars.com/api/?name="
                            + displayName.replace(" ", "+")
                            + "&background=random";

                    return new SmartschoolUserDTO(
                            user.getSmartschoolUid(),
                            displayName,
                            classGroup,
                            schoolName,
                            schoolIdValue, photoUrl);
                })
                .filter(user -> readString(user.name()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || readString(user.smartschoolUserId()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || readString(user.classGroup()).toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(dto -> dto.name().toLowerCase(Locale.ROOT)))
                .limit(20)
                .toList();
    }

    private List<String> normalizeRequestedUids(List<String> rawUids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();

        for (String uid : rawUids) {
            if (uid == null) {
                continue;
            }

            String trimmed = uid.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }
        }

        return new ArrayList<>(normalized);
    }

    private List<String> extractCandidateUids(Map<String, Object> liveUser) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        addIfPresent(candidates, extractLegacyIdentifier(liveUser));
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

    private String extractLegacyIdentifier(Map<String, Object> liveUser) {
        String uid = OneRosterUtils.extractSmartschoolUid(liveUser);
        return uid != null ? uid : "";
    }

    private Long resolveSchoolId(UserEntity actor, Long schoolId) {
        if (actor.getRole() == UserRoles.ADMIN) {
            if (schoolId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht voor admin");
            }
            return schoolId;
        }
        return actor.getSchool().getId();
    }
}