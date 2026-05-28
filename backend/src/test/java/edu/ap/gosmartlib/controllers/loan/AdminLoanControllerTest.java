package edu.ap.gosmartlib.controllers.loan;

import edu.ap.gosmartlib.config.TestSecurityConfig;
import edu.ap.gosmartlib.security.RoleGuard;
import edu.ap.gosmartlib.services.loans.AdminLoanService;
import edu.ap.gosmartlib.services.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AdminLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AdminLoanService adminLoanService;
    @MockitoBean private UserService userService;
    @MockitoBean private RoleGuard roleGuard;

    // ─── GET /loans/school/active ─────────────────────────────────────────────

    @Test
    void givenNullPrincipal_whenGetActiveLoans_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/school/active"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminLoanService);
    }

    @Test
    void givenPrincipalWithoutUserID_whenGetActiveLoans_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/loans/school/active")
                        .with(oauth2Login()))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoLibrarianRole_whenGetActiveLoans_thenReturnsForbidden() throws Exception {
        // roleGuard.isLibrarian() default = false → 403
        mockMvc.perform(get("/loans/school/active")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminLoanService);
    }

    @Test
    void givenValidPrincipal_whenGetActiveLoans_thenCallsServiceWithCorrectParams() throws Exception {
        when(roleGuard.isLibrarian(any())).thenReturn(true);
        when(adminLoanService.getActiveLoansForSchool("uid-1", null, 0, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/loans/school/active")
                        .param("page", "0").param("size", "10")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(adminLoanService).getActiveLoansForSchool("uid-1", null, 0, 10);
    }

    // ─── GET /loans/school/history ────────────────────────────────────────────

    @Test
    void givenNullPrincipal_whenGetLoanHistory_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/school/history"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminLoanService);
    }

    @Test
    void givenValidPrincipal_whenGetLoanHistory_thenCallsServiceWithCorrectParams() throws Exception {
        when(roleGuard.isLibrarian(any())).thenReturn(true);
        when(adminLoanService.getLoanHistoryForSchool("uid-1", 5L, 1, 10)).thenReturn(Page.empty());

        mockMvc.perform(get("/loans/school/history")
                        .param("classId", "5")
                        .param("page", "1").param("size", "10")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(adminLoanService).getLoanHistoryForSchool("uid-1", 5L, 1, 10);
    }

    // ─── GET /loans/school/classes ────────────────────────────────────────────

    @Test
    void givenNullPrincipal_whenGetSchoolClasses_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/school/classes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenValidPrincipal_whenGetSchoolClasses_thenReturnsClasses() throws Exception {
        when(roleGuard.isLibrarian(any())).thenReturn(true);
        when(adminLoanService.getSchoolClasses("uid-1")).thenReturn(List.of());

        mockMvc.perform(get("/loans/school/classes")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(adminLoanService).getSchoolClasses("uid-1");
    }
}
