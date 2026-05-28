package edu.ap.gosmartlib.controllers.loan;

import edu.ap.gosmartlib.dto.loan.LoanPolicyDTO;
import edu.ap.gosmartlib.dto.loan.UpsertLoanPolicyRequest;
import edu.ap.gosmartlib.services.loans.LoanPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanPolicyControllerTest {

    @Mock private LoanPolicyService loanPolicyService;

    @InjectMocks
    private LoanPolicyController loanPolicyController;

    @Test
    void givenSchoolId_whenGetPolicy_thenReturnsOkWithPolicy() {
        LoanPolicyDTO policy = mock(LoanPolicyDTO.class);
        when(loanPolicyService.getPolicy(1L)).thenReturn(policy);

        ResponseEntity<LoanPolicyDTO> response = loanPolicyController.getPolicy(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(policy, response.getBody());
        verify(loanPolicyService).getPolicy(1L);
    }

    @Test
    void givenSchoolIdAndRequest_whenUpsertPolicy_thenReturnsOkWithSaved() {
        UpsertLoanPolicyRequest request = new UpsertLoanPolicyRequest(14, 7, 3);
        LoanPolicyDTO saved = mock(LoanPolicyDTO.class);
        when(loanPolicyService.savePolicy(1L, request)).thenReturn(saved);

        ResponseEntity<LoanPolicyDTO> response = loanPolicyController.upsertPolicy(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(saved, response.getBody());
        verify(loanPolicyService).savePolicy(1L, request);
    }
}
