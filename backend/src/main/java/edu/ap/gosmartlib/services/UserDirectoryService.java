package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.SchoolIntegrationRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterAuthService;
import edu.ap.gosmartlib.services.schoolIntegration.SmartschoolOneRosterClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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

        Long schoolId = actor.getSchool().getId();

        List<UserEntity> knownUsers = userRepository.findAllBySchool_IdAndSmartschoolUidInAndActiveIsTrue(
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

    @SuppressWarnings("unchecked")
    private String extractLegacyIdentifier(Map<String, Object> liveUser) {
        Object metadataObj = liveUser.get("metadata");
        if (!(metadataObj instanceof Map<?, ?> metadata)) {
            return "";
        }

        Object legacyIdentifier = metadata.get("smsc.legacyIdentifier");
        return readString(legacyIdentifier);
    }
}