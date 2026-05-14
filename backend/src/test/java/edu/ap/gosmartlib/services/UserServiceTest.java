package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.repositories.SchoolClassRepository;
import edu.ap.gosmartlib.repositories.SchoolRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAuthenticatedUser_whenLogUserOut_thenInvalidatesSessionAndClearsBothCookies() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.logUserOut(response);

        verify(securityContext).setAuthentication(null);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        assertEquals("JSESSIONID", cookies.get(0).getName());
        assertEquals("", cookies.get(0).getValue());
        assertEquals(0, cookies.get(0).getMaxAge());
        assertEquals("/", cookies.get(0).getPath());

        assertEquals("AUTHENTICATED", cookies.get(1).getName());
        assertEquals("", cookies.get(1).getValue());
        assertEquals(0, cookies.get(1).getMaxAge());
        assertEquals("/", cookies.get(1).getPath());
    }

    @Test
    void givenNoAuthenticatedUser_whenLogUserOut_thenSkipsSessionInvalidationAndStillClearsBothCookies() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        userService.logUserOut(response);

        verify(securityContext, never()).setAuthentication(any());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        assertEquals("JSESSIONID", cookies.get(0).getName());
        assertEquals("AUTHENTICATED", cookies.get(1).getName());
    }

    @Test
    void givenAuthenticatedUser_whenLogUserOut_thenClearedCookiesHaveCorrectProperties() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        userService.logUserOut(response);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        for (Cookie cookie : cookieCaptor.getAllValues()) {
            assertEquals("", cookie.getValue());
            assertEquals(0, cookie.getMaxAge());
            assertEquals("/", cookie.getPath());
            assertEquals(true, cookie.isHttpOnly());
        }
    }
}
