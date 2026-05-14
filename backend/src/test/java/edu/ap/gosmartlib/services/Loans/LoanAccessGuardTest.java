package edu.ap.gosmartlib.services.Loans;

import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.UnauthorizedRoleException;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanAccessGuardTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private LoanAccessGuard accessGuard;

    @Test
    void givenUnknownUid_whenRequireBibliotheekbeheerder_thenThrowsEntityNotFoundException() {
        when(userRepository.findBySmartschoolUid("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accessGuard.requireBibliotheekbeheerder("unknown"));
    }

    @Test
    void givenUserWithoutSchool_whenRequireBibliotheekbeheerder_thenThrowsIllegalArgumentException() {
        UserEntity user = mock(UserEntity.class);
        when(user.getSchool()).thenReturn(null);
        when(userRepository.findBySmartschoolUid("uid")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> accessGuard.requireBibliotheekbeheerder("uid"));
    }

    @Test
    void givenUserWithWrongRole_whenRequireBibliotheekbeheerder_thenThrowsUnauthorizedRoleException() {
        UserEntity user = mock(UserEntity.class);
        when(user.getSchool()).thenReturn(mock(SchoolEntity.class));
        when(user.getRole()).thenReturn(UserRoles.STUDENT);
        when(userRepository.findBySmartschoolUid("uid")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedRoleException.class,
                () -> accessGuard.requireBibliotheekbeheerder("uid"));
    }

    @Test
    void givenValidBibliotheekbeheerder_whenRequireBibliotheekbeheerder_thenReturnsUser() {
        UserEntity user = mock(UserEntity.class);
        when(user.getSchool()).thenReturn(mock(SchoolEntity.class));
        when(user.getRole()).thenReturn(UserRoles.BIBLIOTHEEKBEHEERDER);
        when(userRepository.findBySmartschoolUid("uid")).thenReturn(Optional.of(user));

        UserEntity result = accessGuard.requireBibliotheekbeheerder("uid");

        assertSame(user, result);
    }
}
