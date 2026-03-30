package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocalAuthControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenUnknownRole_whenSwitchRole_thenReturnsBadRequest() {
        UserService userService = mock(UserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        LocalAuthController controller = new LocalAuthController(userService, userRepository);

        ResponseEntity<String> response = controller.switchRole(
                "unknown",
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertEquals(400, response.getStatusCode().value());
        verify(userService, never()).syncUser(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void givenBibliotheekbeheerderRole_whenSwitchRole_thenUpdatesUserRoleAndAuthenticates() {
        UserService userService = mock(UserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        LocalAuthController controller = new LocalAuthController(userService, userRepository);

        UserEntity user = new UserEntity();
        user.setRole(UserRoles.STUDENT);
        when(userRepository.findBySmartschoolUid("mock-librarian-local")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        session.setAttribute("existing", "session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<String> result = controller.switchRole("bibliotheekbeheerder", request, response);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, times(2)).syncUser(any());
        verify(userRepository, times(1)).findBySmartschoolUid("mock-librarian-local");
        verify(userRepository, times(1)).save(argThat(saved -> saved.getRole() == UserRoles.BIBLIOTHEEKBEHEERDER));
    }

    @Test
    void givenLeerlingRole_whenSwitchRole_thenAuthenticatesAndDoesNotTouchRepository() {
        UserService userService = mock(UserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        LocalAuthController controller = new LocalAuthController(userService, userRepository);

        ResponseEntity<String> result = controller.switchRole(
                "leerling",
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, times(2)).syncUser(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void givenLeerkrachtRole_whenSwitchRole_thenAuthenticatesAndDoesNotTouchRepository() {
        UserService userService = mock(UserService.class);
        UserRepository userRepository = mock(UserRepository.class);
        LocalAuthController controller = new LocalAuthController(userService, userRepository);

        ResponseEntity<String> result = controller.switchRole(
                "leerkracht",
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, times(2)).syncUser(any());
        verifyNoInteractions(userRepository);
    }
}

