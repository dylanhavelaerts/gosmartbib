package edu.ap.gosmartlib.controllers.loanControllers;

import edu.ap.gosmartlib.dto.loan.LoanPolicyDTO;
import edu.ap.gosmartlib.dto.loan.UpsertLoanPolicyRequest;
import edu.ap.gosmartlib.services.loans.LoanPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/schools/{schoolId}/loan-policy")
@RequiredArgsConstructor
public class LoanPolicyController {

    private final LoanPolicyService loanPolicyService;

    @GetMapping
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<LoanPolicyDTO> getPolicy(@PathVariable Long schoolId) {
        return ResponseEntity.ok(loanPolicyService.getPolicy(schoolId));
    }

    @PutMapping
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<LoanPolicyDTO> upsertPolicy(@PathVariable Long schoolId, @RequestBody UpsertLoanPolicyRequest request) {
        return ResponseEntity.ok(loanPolicyService.savePolicy(schoolId, request));
    }
}
