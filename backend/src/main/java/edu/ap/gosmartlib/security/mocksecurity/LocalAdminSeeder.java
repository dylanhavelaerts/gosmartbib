package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.repositories.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Maakt een lokaal adminaccount aan bij opstart voor ontwikkeling.
 *
 * Het wachtwoord wordt ingelezen als plaintext uit de omgevingsvariabele
 * LOCAL_ADMIN_PASSWORD en bij aanmaak gehasht via BCrypt. Alleen actief in
 * het local-profiel.
 */
@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalAdminSeeder {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${LOCAL_ADMIN_PASSWORD}")
    private String localAdminPassword;

    /**
     * Voert de seed uit na het opstarten van de applicatiecontext.
     * Slaat het account over als de gebruikersnaam "admin" al aanwezig is.
     */
    @PostConstruct
    public void seed() {
        if (adminRepository.findByUsername("admin").isEmpty()) {
            AdminEntity admin = new AdminEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(localAdminPassword));
            adminRepository.save(admin);
        }
    }
}
