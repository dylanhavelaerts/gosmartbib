package edu.ap.gosmartlib.controllers.LoanControllers;

import edu.ap.gosmartlib.dto.loan.AdminActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.AdminLoanHistoryDTO;
import edu.ap.gosmartlib.services.Loans.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    @GetMapping("/school/active")
    public ResponseEntity<List<AdminActiveLoanDTO>> getActiveLoansForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String actorUid = (String) principal.getAttribute("userID");
        return ResponseEntity.ok(adminLoanService.getActiveLoansForSchool(actorUid, classId));
    }

    @GetMapping("/school/history")
    public ResponseEntity<List<AdminLoanHistoryDTO>> getLoanHistoryForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String actorUid = (String) principal.getAttribute("userID");
        return ResponseEntity.ok(adminLoanService.getLoanHistoryForSchool(actorUid, classId));
    }
}
