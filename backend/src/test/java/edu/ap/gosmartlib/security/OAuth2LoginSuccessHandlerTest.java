package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.services.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenValidOAuthUser_whenAuthenticationSuccess_thenSetsRoleAndRedirects() throws Exception {
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(userService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("userID", "u-1"),
                "userID");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "smartschool");

        UserEntity syncedUser = new UserEntity();
        syncedUser.setRole(UserRoles.ADMIN);
        when(userService.syncUser(oauth2User)).thenReturn(syncedUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(Boolean.TRUE, request.getSession(false).getAttribute("authenticated"));
        assertEquals("http://localhost:3000/", response.getRedirectedUrl());

        Authentication enriched = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(enriched);
        List<String> roles = enriched.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(roles.contains("ROLE_ADMIN"));
        verify(userService, times(1)).syncUser(oauth2User);
    }

    @Test
    void givenSyncFails_whenAuthenticationSuccess_thenRedirectsToLoginError() throws Exception {
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(userService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("userID", "u-1"),
                "userID");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "smartschool");

        when(userService.syncUser(oauth2User)).thenThrow(new RuntimeException("db down"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("http://localhost:3000/login?error=true", response.getRedirectedUrl());
        verify(userService, times(1)).syncUser(oauth2User);
    }

    @Test
    void givenBlankFrontendUrl_whenAuthenticationSuccess_thenRedirectsToRoot() throws Exception {
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(userService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "");

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("userID", "u-1"),
                "userID");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "smartschool");

        UserEntity syncedUser = new UserEntity();
        syncedUser.setRole(UserRoles.BIBLIOTHEEKBEHEERDER);
        when(userService.syncUser(oauth2User)).thenReturn(syncedUser);

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertEquals("/", response.getRedirectedUrl());
    }
}

