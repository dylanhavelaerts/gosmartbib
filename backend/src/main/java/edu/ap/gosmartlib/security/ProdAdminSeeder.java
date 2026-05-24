package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.repositories.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;


@Profile("prod")
@Component
@RequiredArgsConstructor
public class ProdAdminSeeder {

    private final AdminRepository adminRepository;

    @Value("${admin.seed.username}")
    private String seedUsername;

    @Value("${admin.seed.password-hash}")
    private String seedPasswordHash;

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
