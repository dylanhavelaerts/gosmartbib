package edu.ap.gosmartlib.security.mocksecurity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class MockAuth extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain) throws ServletException, IOException {

                // Inject een mock gebruiker in de security context als er nog geen
                // authenticatie is
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        Map<String, Object> attributes = Map.of(
                                        "userID", "mock-user-local-dev",
                                        "name", "Local",
                                        "surname", "Developer",
                                        "fullname", "Developer Local",
                                        "username", "localdev",
                                        "platform", "https://aphogeschool.smartschool.be",
                                        "isMainAccount", 1,
                                        "isCoAccount", 0);

                        OAuth2User mockUser = new DefaultOAuth2User(
                                        Collections.emptyList(),
                                        attributes,
                                        "userID");

                        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                                        mockUser,
                                        Collections.emptyList(),
                                        "smartschool");

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
        }
}
