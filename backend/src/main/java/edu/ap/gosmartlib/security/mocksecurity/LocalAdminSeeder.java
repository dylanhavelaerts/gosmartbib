package edu.ap.gosmartlib.security.mocksecurity;

import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.repositories.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalAdminSeeder {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void seed() {
        if (adminRepository.findByUsername("admin").isEmpty()) {
            AdminEntity admin = new AdminEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            adminRepository.save(admin);
        }
    }
}
