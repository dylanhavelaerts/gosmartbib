package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.services.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MockAuthTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthentication_whenFilterRuns_thenInjectsMockUserAndSyncs() throws Exception {
        UserService userService = mock(UserService.class);
        FilterChain chain = mock(FilterChain.class);
        MockAuth filter = new MockAuth(userService);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, times(1)).syncUser(any());
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void givenExistingAuthentication_whenFilterRuns_thenDoesNotSyncAgain() throws Exception {
        UserService userService = mock(UserService.class);
        FilterChain chain = mock(FilterChain.class);
        MockAuth filter = new MockAuth(userService);

        SecurityContextHolder.getContext().setAuthentication(mock(org.springframework.security.core.Authentication.class));

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(userService, never()).syncUser(any());
        verify(chain, times(1)).doFilter(any(), any());
    }
}

