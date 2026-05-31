package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.repositories.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Laadt platformadministrators uit de database voor Spring Security.
 *
 * Implementeert UserDetailsService zodat DaoAuthenticationProvider de admin
 * opzoekt op gebruikersnaam. Bij onbekende gebruikersnaam wordt een
 * UsernameNotFoundException gegooid die Spring Security intern vertaalt naar
 * een 401-respons.
 */
@Component
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    /**
     * @param username de opgegeven gebruikersnaam
     * @return AdminPrincipal voor de gevonden admin
     * @throws UsernameNotFoundException als er geen admin met die gebruikersnaam bestaat
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminRepository.findByUsername(username)
                .map(AdminPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
    }
}
