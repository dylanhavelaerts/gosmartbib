package edu.ap.gosmartlib.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class AdminLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();
    private final LoginAttemptService loginAttemptService;

    public AdminLoginFilter(AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/login"));
        setAuthenticationManager(authenticationManager);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String ip = request.getRemoteAddr();
        if (!loginAttemptService.tryConsume(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return null;
        }
        if (!request.getContentType().startsWith("application/json")) {
            throw new AuthenticationServiceException("Content-Type must be application/json");
        }
        try {
            AdminLoginRequest body = new ObjectMapper()
                    .readValue(request.getInputStream(), AdminLoginRequest.class);
            String username = body.username() != null ? body.username().trim() : "";
            String password = body.password() != null ? body.password() : "";
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (IOException e) {
            throw new AuthenticationServiceException("Kon login-request niet lezen", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        loginAttemptService.reset(request.getRemoteAddr());
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

    private record AdminLoginRequest(String username, String password) {}
}
