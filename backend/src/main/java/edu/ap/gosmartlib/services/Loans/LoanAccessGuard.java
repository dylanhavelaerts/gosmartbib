package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.UnauthorizedRoleException;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanAccessGuard {

    private final UserRepository userRepository;

    public UserEntity requireBibliotheekbeheerder(String actorUid) {
        UserEntity actor = userRepository.findBySmartschoolUid(actorUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden."));
        if (actor.getSchool() == null)
            throw new IllegalArgumentException("De gebruiker heeft geen school gekoppeld.");
        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER)
            throw new UnauthorizedRoleException("Alleen bibliotheekbeheerders hebben toegang tot dit overzicht.");
        return actor;
    }
}
