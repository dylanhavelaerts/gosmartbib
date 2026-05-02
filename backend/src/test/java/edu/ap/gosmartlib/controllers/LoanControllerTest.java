package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.controllers.LoanControllers.LoanController;
import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.services.Loans.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

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

    @Test
    void givenSmartschoolUserId_whenGetActiveLoans_thenReturnsOkWithLoans() {
        // Arrange
        String uid = "uid-123";
        List<ActiveLoanDTO> expectedLoans = List.of(mock(ActiveLoanDTO.class));
        when(loanService.getActiveLoansByUser(uid)).thenReturn(expectedLoans);

        // Act
        ResponseEntity<List<ActiveLoanDTO>> response = loanController.getActiveLoans(uid);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedLoans, response.getBody());
        verify(loanService, times(1)).getActiveLoansByUser(uid);
    }

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
}