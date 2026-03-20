package edu.ap.testbackend.security.mocksecurity;

import edu.ap.testbackend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("local") // Security config voor lokaal, met mock auth en geen CORS
@RequiredArgsConstructor
public class LocalSecurityConfig {

    private final UserService userService;

    @Bean
    public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new MockAuth(userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
