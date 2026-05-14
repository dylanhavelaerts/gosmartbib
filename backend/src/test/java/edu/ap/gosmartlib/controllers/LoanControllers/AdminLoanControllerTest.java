package edu.ap.gosmartlib.controllers.LoanControllers;

import edu.ap.gosmartlib.controllers.LoanControllers.AdminLoanController;
import edu.ap.gosmartlib.dto.loan.AdminActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.AdminLoanHistoryDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.services.Loans.AdminLoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoanControllerTest {

    @Mock private AdminLoanService adminLoanService;
    @Mock private OAuth2User principal;
    @InjectMocks private AdminLoanController adminLoanController;

    // --- getActiveLoansForSchool ---

    @Test
    void givenNullPrincipal_whenGetActiveLoans_thenReturnsUnauthorized() {
        ResponseEntity<Page<AdminActiveLoanDTO>> response =
                adminLoanController.getActiveLoansForSchool(null, null, 0, 10);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(adminLoanService);
    }

    @Test
    void givenPrincipalWithoutUserID_whenGetActiveLoans_thenReturnsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn(null);

        ResponseEntity<Page<AdminActiveLoanDTO>> response =
                adminLoanController.getActiveLoansForSchool(principal, null, 0, 10);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void givenValidPrincipal_whenGetActiveLoans_thenCallsServiceWithCorrectParams() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");
        when(adminLoanService.getActiveLoansForSchool("uid-1", null, 0, 10)).thenReturn(Page.empty());

        ResponseEntity<Page<AdminActiveLoanDTO>> response =
                adminLoanController.getActiveLoansForSchool(principal, null, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminLoanService).getActiveLoansForSchool("uid-1", null, 0, 10);
    }

    // --- getLoanHistoryForSchool ---

    @Test
    void givenNullPrincipal_whenGetLoanHistory_thenReturnsUnauthorized() {
        ResponseEntity<Page<AdminLoanHistoryDTO>> response =
                adminLoanController.getLoanHistoryForSchool(null, null, 0, 10);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(adminLoanService);
    }

    @Test
    void givenValidPrincipal_whenGetLoanHistory_thenCallsServiceWithCorrectParams() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");
        when(adminLoanService.getLoanHistoryForSchool("uid-1", 5L, 1, 10)).thenReturn(Page.empty());

        ResponseEntity<Page<AdminLoanHistoryDTO>> response =
                adminLoanController.getLoanHistoryForSchool(principal, 5L, 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminLoanService).getLoanHistoryForSchool("uid-1", 5L, 1, 10);
    }

    // --- getSchoolClasses ---

    @Test
    void givenNullPrincipal_whenGetSchoolClasses_thenReturnsUnauthorized() {
        ResponseEntity<List<ReadingListAssignmentTargetsDTO.ClassTarget>> response =
                adminLoanController.getSchoolClasses(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void givenValidPrincipal_whenGetSchoolClasses_thenReturnsClasses() {
        when(principal.getAttribute("userID")).thenReturn("uid-1");
        when(adminLoanService.getSchoolClasses("uid-1")).thenReturn(List.of());

        ResponseEntity<List<ReadingListAssignmentTargetsDTO.ClassTarget>> response =
                adminLoanController.getSchoolClasses(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminLoanService).getSchoolClasses("uid-1");
    }
}
