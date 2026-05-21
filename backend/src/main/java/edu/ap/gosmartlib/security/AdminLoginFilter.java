package edu.ap.gosmartlib.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

public class AdminLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AdminLoginFilter(AuthenticationManager authenticationManager) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/login"));
        setAuthenticationManager(authenticationManager);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String username = request.getParameter("username") != null
                ? request.getParameter("username").trim() : "";
        String password = request.getParameter("password") != null
                ? request.getParameter("password") : "";
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

//    private record AdminLoginRequest(String username, String password) {}
}
