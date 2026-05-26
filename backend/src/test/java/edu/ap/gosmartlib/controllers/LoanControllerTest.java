package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.controllers.loan.LoanController;
import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanHistoryDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.config.TestSecurityConfig;
import edu.ap.gosmartlib.services.loans.LoanDueDateNotificationService;
import edu.ap.gosmartlib.services.loans.LoanPolicyService;
import edu.ap.gosmartlib.services.loans.LoanService;
import edu.ap.gosmartlib.services.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoanService loanService;
    @MockitoBean private LoanPolicyService loanPolicyService;
    @MockitoBean private LoanDueDateNotificationService loanDueDateNotificationService;
    @MockitoBean private UserService userService;

    // ─── POST /loans ──────────────────────────────────────────────────────────

    @Test
    void givenValidRequests_whenCreateLoans_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"bookId\":1,\"quantity\":1,\"user\":null}]")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(loanService).createLoans(anyList());
    }

    // ─── GET /loans/active ────────────────────────────────────────────────────

    @Test
    void givenValidPrincipal_whenGetActiveLoans_thenReturnsOkWithLoans() throws Exception {
        when(loanService.getActiveLoansByUser("uid-123")).thenReturn(List.of());

        mockMvc.perform(get("/loans/active")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-123"))))
                .andExpect(status().isOk());

        verify(loanService).getActiveLoansByUser("uid-123");
    }

    @Test
    void givenNullPrincipal_whenGetActiveLoans_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/active"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    @Test
    void givenAdminWithOtherUserId_whenGetActiveLoans_thenCallsGetActiveLoansAsAdmin() throws Exception {
        when(loanService.getActiveLoansAsAdmin("beheerder-123", "lener-456")).thenReturn(List.of());

        mockMvc.perform(get("/loans/active")
                        .param("smartschoolUserId", "lener-456")
                        .with(oauth2Login().attributes(a -> a.put("userID", "beheerder-123"))))
                .andExpect(status().isOk());

        verify(loanService).getActiveLoansAsAdmin("beheerder-123", "lener-456");
        verify(loanService, never()).getActiveLoansByUser(any());
    }

    @Test
    void givenAdminWithOwnUserId_whenGetActiveLoans_thenCallsGetActiveLoansByUser() throws Exception {
        when(loanService.getActiveLoansByUser("beheerder-123")).thenReturn(List.of());

        mockMvc.perform(get("/loans/active")
                        .param("smartschoolUserId", "beheerder-123")
                        .with(oauth2Login().attributes(a -> a.put("userID", "beheerder-123"))))
                .andExpect(status().isOk());

        verify(loanService).getActiveLoansByUser("beheerder-123");
        verify(loanService, never()).getActiveLoansAsAdmin(any(), any());
    }

    // ─── POST /loans/return ───────────────────────────────────────────────────

    @Test
    void givenValidRequests_whenReturnBooksBulk_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"bookId\":1,\"quantity\":1,\"smartschoolUserId\":\"uid-1\"}]")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(loanService).returnBooksBulk(anyList());
    }

    // ─── POST /loans/{loanId}/return ──────────────────────────────────────────

    @Test
    void givenValidRequest_whenReturnBook_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans/1/return")
                        .param("quantity", "1")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(loanService).returnBook(1L, 1);
    }

    // ─── GET /loans/history ───────────────────────────────────────────────────

    @Test
    void givenValidPrincipal_whenGetLoanHistory_thenReturnsOkWithHistory() throws Exception {
        when(loanService.getLoanHistoryByUser(eq("uid-123"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/loans/history")
                        .param("page", "0").param("size", "10")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-123"))))
                .andExpect(status().isOk());

        verify(loanService).getLoanHistoryByUser(eq("uid-123"), any());
    }

    @Test
    void givenNullPrincipal_whenGetLoanHistory_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/history"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    // ─── POST /loans/{loanId}/extension-request ───────────────────────────────

    @Test
    void givenValidPrincipal_whenRequestLoanExtension_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-123"))))
                .andExpect(status().isOk());

        verify(loanService).requestLoanExtension(1L, "uid-123");
    }

    @Test
    void givenNullPrincipal_whenRequestLoanExtension_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    // ─── GET /loans/extension-requests/pending ────────────────────────────────

    @Test
    void givenValidPrincipal_whenGetPendingExtensionRequests_thenReturnsOkWithRequests() throws Exception {
        when(loanService.getPendingExtensionRequestsForSchool("beheerder-123")).thenReturn(List.of());

        mockMvc.perform(get("/loans/extension-requests/pending")
                        .with(oauth2Login().attributes(a -> a.put("userID", "beheerder-123"))))
                .andExpect(status().isOk());

        verify(loanService).getPendingExtensionRequestsForSchool("beheerder-123");
    }

    @Test
    void givenNullPrincipal_whenGetPendingExtensionRequests_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/extension-requests/pending"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    // ─── POST /loans/{loanId}/extension-request/approve ──────────────────────

    @Test
    void givenValidPrincipal_whenApproveLoanExtension_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request/approve")
                        .with(oauth2Login().attributes(a -> a.put("userID", "beheerder-123"))))
                .andExpect(status().isOk());

        verify(loanService).approveLoanExtension(1L, "beheerder-123");
    }

    @Test
    void givenNullPrincipal_whenApproveLoanExtension_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request/approve"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    // ─── POST /loans/{loanId}/extension-request/deny ─────────────────────────

    @Test
    void givenValidPrincipal_whenDenyLoanExtension_thenReturnsOk() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request/deny")
                        .with(oauth2Login().attributes(a -> a.put("userID", "beheerder-123"))))
                .andExpect(status().isOk());

        verify(loanService).denyLoanExtension(1L, "beheerder-123");
    }

    @Test
    void givenNullPrincipal_whenDenyLoanExtension_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/loans/1/extension-request/deny"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    // ─── GET /loans/reminder-days ─────────────────────────────────────────────

    @Test
    void givenValidPrincipal_whenGetReminderDays_thenReturnsOkWithDays() throws Exception {
        when(loanPolicyService.getReminderDaysForUser("uid-123")).thenReturn(5);

        mockMvc.perform(get("/loans/reminder-days")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));

        verify(loanPolicyService).getReminderDaysForUser("uid-123");
    }

    @Test
    void givenNullPrincipal_whenGetReminderDays_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loans/reminder-days"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanPolicyService);
    }
}
