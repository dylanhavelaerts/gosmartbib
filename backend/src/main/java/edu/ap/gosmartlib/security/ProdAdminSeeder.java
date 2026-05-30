package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.repositories.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Maakt het eerste platformadminaccount aan bij opstart in productie.
 *
 * De gebruikersnaam en de vooraf gegenereerde BCrypt-hash worden ingelezen uit de
 * applicatieconfiguratie. Als het account al bestaat, wordt er niets gedaan.
 * De seeder is idempotent en veilig bij herstarts.
 */
@Profile("prod")
@Component
@RequiredArgsConstructor
public class ProdAdminSeeder {

    private final AdminRepository adminRepository;

    @Value("${admin.seed.username}")
    private String seedUsername;

    @Value("${admin.seed.password-hash}")
    private String seedPasswordHash;

    /**
     * Voert de seed uit na het opstarten van de applicatiecontext.
     * Slaat het account over als de gebruikersnaam al aanwezig is in de database.
     */
    @PostConstruct
    public void seed() {
        if (adminRepository.findByUsername(seedUsername).isEmpty()) {
            AdminEntity admin = new AdminEntity();
            admin.setUsername(seedUsername);
            admin.setPasswordHash(seedPasswordHash);
            adminRepository.save(admin);
        }
    }
}
