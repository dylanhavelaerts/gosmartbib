package edu.ap.testbackend.security.mocksecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("local") // Security config voor lokaal, met mock auth en geen CORS
public class LocalSecurityConfig {
    @Bean
    public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // sta alles lokaal toe
                )
                .addFilterBefore(new MockAuth(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
