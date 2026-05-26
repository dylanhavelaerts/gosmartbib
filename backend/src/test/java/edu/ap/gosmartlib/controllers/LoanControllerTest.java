package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.controllers.loan.LoanController;
import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanHistoryDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.services.loans.LoanPolicyService;
import edu.ap.gosmartlib.services.loans.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @Mock
    private OAuth2User principal;

    @InjectMocks
    private LoanController loanController;

    @Mock
    private LoanPolicyService loanPolicyService;

    @Test
    void givenValidRequests_whenCreateLoans_thenReturnsOk() {
        // Arrange
        List<LoanRequestDTO> requests = List.of(mock(LoanRequestDTO.class));

        // Act
        ResponseEntity<Void> response = loanController.createLoans(requests);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).createLoans(requests);
    }

    // --- Tests voor getActiveLoans ---

    @Test
    void givenValidPrincipal_whenGetActiveLoans_thenReturnsOkWithLoans() {
        String uid = "uid-123";
        List<ActiveLoanDTO> expectedLoans = List.of(mock(ActiveLoanDTO.class));

        when(principal.getAttribute("userID")).thenReturn(uid);
        when(loanService.getActiveLoansByUser(uid)).thenReturn(expectedLoans);

        // null = geen smartschoolUserId meegegeven (student bekijkt eigen leningen)
        ResponseEntity<List<ActiveLoanDTO>> response = loanController.getActiveLoans(principal, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedLoans, response.getBody());
        verify(loanService, times(1)).getActiveLoansByUser(uid);
    }

    @Test
    void givenNullPrincipal_whenGetActiveLoans_thenReturnsUnauthorized() {
        ResponseEntity<List<ActiveLoanDTO>> response = loanController.getActiveLoans(null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanService);
    }

    @Test
    void givenAdminWithOtherUserId_whenGetActiveLoans_thenCallsGetActiveLoansAsAdmin() {
        String adminUid = "beheerder-123";
        String targetUid = "lener-456";
        List<ActiveLoanDTO> expectedLoans = List.of(mock(ActiveLoanDTO.class));

        when(principal.getAttribute("userID")).thenReturn(adminUid);
        when(loanService.getActiveLoansAsAdmin(adminUid, targetUid)).thenReturn(expectedLoans);

        ResponseEntity<List<ActiveLoanDTO>> response = loanController.getActiveLoans(principal, targetUid);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedLoans, response.getBody());
        verify(loanService, times(1)).getActiveLoansAsAdmin(adminUid, targetUid);
        verify(loanService, never()).getActiveLoansByUser(any());
    }

    @Test
    void givenAdminWithOwnUserId_whenGetActiveLoans_thenCallsGetActiveLoansByUser() {
        String uid = "beheerder-123";
        List<ActiveLoanDTO> expectedLoans = List.of(mock(ActiveLoanDTO.class));

        when(principal.getAttribute("userID")).thenReturn(uid);
        when(loanService.getActiveLoansByUser(uid)).thenReturn(expectedLoans);

        // smartschoolUserId == eigen uid → geen admin-pad nodig
        ResponseEntity<List<ActiveLoanDTO>> response = loanController.getActiveLoans(principal, uid);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).getActiveLoansByUser(uid);
        verify(loanService, never()).getActiveLoansAsAdmin(any(), any());
    }

    // --- Tests voor Return endpoints ---

    @Test
    void givenValidRequests_whenReturnBooksBulk_thenReturnsOk() {
        // Arrange
        List<ReturnBulkRequestDTO> requests = List.of(mock(ReturnBulkRequestDTO.class));

        // Act
        ResponseEntity<Void> response = loanController.returnBooksBulk(requests);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).returnBooksBulk(requests);
    }

    @Test
    void givenValidRequest_whenReturnBook_thenReturnsOk() {
        // Arrange
        Long loanId = 1L;
        int quantity = 1;

        // Act
        ResponseEntity<Void> response = loanController.returnBook(loanId, quantity);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).returnBook(loanId, quantity);
    }

    // --- Tests voor getLoanHistory ---

    @Test
    void givenValidPrincipal_whenGetLoanHistory_thenReturnsOkWithHistory() {
        String uid = "uid-123";
        Page<LoanHistoryDTO> expectedPage = new PageImpl<>(List.of(mock(LoanHistoryDTO.class)));

        when(principal.getAttribute("userID")).thenReturn(uid);
        when(loanService.getLoanHistoryByUser(eq(uid), any(Pageable.class))).thenReturn(expectedPage);

        ResponseEntity<Page<LoanHistoryDTO>> response = loanController.getLoanHistory(principal, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedPage, response.getBody());
        verify(loanService, times(1)).getLoanHistoryByUser(eq(uid), any(Pageable.class));
    }

    @Test
    void givenNullPrincipal_whenGetLoanHistory_thenThrowsException() {
        assertThrows(Exception.class, () -> loanController.getLoanHistory(null, 0, 10));
        verifyNoInteractions(loanService);
    }

    // --- Tests voor verlengingsaanvragen ---

    @Test
    void givenValidPrincipal_whenRequestLoanExtension_thenReturnsOk() {
        // Arrange
        Long loanId = 1L;
        String uid = "uid-123";

        when(principal.getAttribute("userID")).thenReturn(uid);

        // Act
        ResponseEntity<Void> response = loanController.requestLoanExtension(loanId, principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).requestLoanExtension(loanId, uid);
    }

    @Test
    void givenNullPrincipal_whenRequestLoanExtension_thenReturnsUnauthorized() {
        // Act
        ResponseEntity<Void> response = loanController.requestLoanExtension(1L, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanService);
    }

    @Test
    void givenValidPrincipal_whenGetPendingExtensionRequests_thenReturnsOkWithRequests() {
        // Arrange
        String uid = "beheerder-123";
        List<LoanExtensionRequestDTO> expectedRequests = List.of(mock(LoanExtensionRequestDTO.class));

        when(principal.getAttribute("userID")).thenReturn(uid);
        when(loanService.getPendingExtensionRequestsForSchool(uid)).thenReturn(expectedRequests);

        // Act
        ResponseEntity<List<LoanExtensionRequestDTO>> response = loanController.getPendingExtensionRequests(principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedRequests, response.getBody());
        verify(loanService, times(1)).getPendingExtensionRequestsForSchool(uid);
    }

    @Test
    void givenNullPrincipal_whenGetPendingExtensionRequests_thenReturnsUnauthorized() {
        // Act
        ResponseEntity<List<LoanExtensionRequestDTO>> response = loanController.getPendingExtensionRequests(null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanService);
    }

    @Test
    void givenValidPrincipal_whenApproveLoanExtension_thenReturnsOk() {
        // Arrange
        Long loanId = 1L;
        String uid = "beheerder-123";

        when(principal.getAttribute("userID")).thenReturn(uid);

        // Act
        ResponseEntity<Void> response = loanController.approveLoanExtension(loanId, principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).approveLoanExtension(loanId, uid);
    }

    @Test
    void givenNullPrincipal_whenApproveLoanExtension_thenReturnsUnauthorized() {
        // Act
        ResponseEntity<Void> response = loanController.approveLoanExtension(1L, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanService);
    }

    @Test
    void givenValidPrincipal_whenDenyLoanExtension_thenReturnsOk() {
        // Arrange
        Long loanId = 1L;
        String uid = "beheerder-123";

        when(principal.getAttribute("userID")).thenReturn(uid);

        // Act
        ResponseEntity<Void> response = loanController.denyLoanExtension(loanId, principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).denyLoanExtension(loanId, uid);
    }

    @Test
    void givenNullPrincipal_whenDenyLoanExtension_thenReturnsUnauthorized() {
        // Act
        ResponseEntity<Void> response = loanController.denyLoanExtension(1L, null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanService);
    }
    @Test
    void givenValidPrincipal_whenGetReminderDays_thenReturnsOkWithDays() {
        String uid = "uid-123";
        when(principal.getAttribute("userID")).thenReturn(uid);
        when(loanPolicyService.getReminderDaysForUser(uid)).thenReturn(5);

        ResponseEntity<Integer> response = loanController.getReminderDays(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody());
        verify(loanPolicyService, times(1)).getReminderDaysForUser(uid);
    }

    @Test
    void givenNullPrincipal_whenGetReminderDays_thenReturnsUnauthorized() {
        ResponseEntity<Integer> response = loanController.getReminderDays(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(loanPolicyService);
    }

}