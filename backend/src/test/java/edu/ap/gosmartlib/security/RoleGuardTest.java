package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleGuardTest {

    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;

    @InjectMocks
    private RoleGuard roleGuard;

    // ─── isAdmin ──────────────────────────────────────────────────────────────

    @Test
    void givenAdminPrincipal_whenIsAdmin_thenReturnsTrue() {
        when(authentication.getPrincipal()).thenReturn(mock(AdminPrincipal.class));
        assertTrue(roleGuard.isAdmin(authentication));
    }

    @Test
    void givenOAuth2Principal_whenIsAdmin_thenReturnsFalse() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        assertFalse(roleGuard.isAdmin(authentication));
    }

    @Test
    void givenNullAuthentication_whenIsAdmin_thenReturnsFalse() {
        assertFalse(roleGuard.isAdmin(null));
    }

    // ─── isBibbeheerder ───────────────────────────────────────────────────────

    @Test
    void givenBibbeheerder_whenIsBibbeheerder_thenReturnsTrue() {
        UserEntity user = new UserEntity();
        user.setRole(UserRoles.BIBLIOTHEEKBEHEERDER);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));

        assertTrue(roleGuard.isBibbeheerder(authentication));
    }

    @Test
    void givenStudent_whenIsBibbeheerder_thenReturnsFalse() {
        UserEntity user = new UserEntity();
        user.setRole(UserRoles.STUDENT);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));

        assertFalse(roleGuard.isBibbeheerder(authentication));
    }

    @Test
    void givenNullAuthentication_whenIsBibbeheerder_thenReturnsFalse() {
        assertFalse(roleGuard.isBibbeheerder(null));
    }

    @Test
    void givenMissingUserID_whenIsBibbeheerder_thenReturnsFalse() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn(null);

        assertFalse(roleGuard.isBibbeheerder(authentication));
        verifyNoInteractions(userRepository);
    }

    @Test
    void givenUserNotFound_whenIsBibbeheerder_thenReturnsFalse() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.empty());

        assertFalse(roleGuard.isBibbeheerder(authentication));
    }

    // ─── isTeacherOrBibbeheerder ──────────────────────────────────────────────

    @Test
    void givenTeacher_whenIsTeacherOrBibbeheerder_thenReturnsTrue() {
        UserEntity user = new UserEntity();
        user.setRole(UserRoles.TEACHER);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));

        assertTrue(roleGuard.isTeacherOrBibbeheerder(authentication));
    }

    @Test
    void givenStudent_whenIsTeacherOrBibbeheerder_thenReturnsFalse() {
        UserEntity user = new UserEntity();
        user.setRole(UserRoles.STUDENT);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("userID")).thenReturn("uid-1");
        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));

        assertFalse(roleGuard.isTeacherOrBibbeheerder(authentication));
    }
}
