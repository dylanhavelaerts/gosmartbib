package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.AdminEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal voor platformadministrators.
 *
 * Wraps een AdminEntity als UserDetails zodat DaoAuthenticationProvider de
 * admin kan authenticeren. Het onderscheid met OAuth2User is bewust: de
 * isAdmin-controle in RoleGuard werkt puur op instanceof AdminPrincipal,
 * zonder extra databasequery.
 */
public class AdminPrincipal implements UserDetails {

    private final AdminEntity admin;

    public AdminPrincipal(AdminEntity admin) {
        this.admin = admin;
    }

    public Long getId() {
        return admin.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return admin.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return admin.getUsername();
    }
}
