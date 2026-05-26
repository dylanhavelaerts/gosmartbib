package edu.ap.gosmartlib.security;

import edu.ap.gosmartlib.entities.AdminEntity;
import edu.ap.gosmartlib.repositories.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock private AdminRepository adminRepository;
    @InjectMocks private AdminUserDetailsService service;

    @Test
    void givenExistingAdmin_whenLoadUserByUsername_thenReturnsAdminPrincipal() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(new AdminEntity()));

        UserDetails result = service.loadUserByUsername("admin");

        assertInstanceOf(AdminPrincipal.class, result);
    }

    @Test
    void givenUnknownAdmin_whenLoadUserByUsername_thenThrowsUsernameNotFoundException() {
        when(adminRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }
}
