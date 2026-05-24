package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.dto.user.UserPreferenceDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserRepository userRepository;

    public UserPreferenceDTO getPreferences(String smartschoolUid) {
        UserEntity user = findUser(smartschoolUid);
        return new UserPreferenceDTO(user.isAnonymousLeaderboard());
    }

    public UserPreferenceDTO updatePreferences(String smartschoolUid, UserPreferenceDTO dto) {
        UserEntity user = findUser(smartschoolUid);
        user.setAnonymousLeaderboard(dto.anonymousLeaderboard());
        userRepository.save(user);
        return new UserPreferenceDTO(user.isAnonymousLeaderboard());
    }

    private UserEntity findUser(String smartschoolUid) {
        return userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
    }
}
