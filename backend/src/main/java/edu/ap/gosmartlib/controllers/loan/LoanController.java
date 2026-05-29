package edu.ap.gosmartlib.controllers.loan;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.dto.loan.LoanHistoryDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.loans.LoanDueDateNotificationService;
import edu.ap.gosmartlib.services.loans.LoanPolicyService;
import edu.ap.gosmartlib.services.loans.LoanService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

/**
 * REST-controller voor leningbeheer vanuit gebruikers- en bibliotheekbeheerderersperspectief.
 * Bibliotheekbeheerder-endpoints zijn beveiligd via @PreAuthorize met roleGuard.isLibrarian.
 */
@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;
    private final LoanPolicyService loanPolicyService;
    private final AuthHelper authHelper;
    private final LoanDueDateNotificationService loanDueDateNotificationService;



    @PostMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> createLoans(@RequestBody List<LoanRequestDTO> requests) {
        loanService.createLoans(requests);
        return ResponseEntity.ok().build();
    }

    /**
     * Geeft actieve leningen terug.
     * Als een bibliotheekbeheerder een andere smartschoolUserId meegeeft, worden de leningen van die gebruiker teruggegeven.
     * Zonder parameter geeft het de leningen van de ingelogde gebruiker terug.
     */
    @GetMapping("/active")
    public ResponseEntity<List<ActiveLoanDTO>> getActiveLoans(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String smartschoolUserId) {

        String smartschoolUid = authHelper.extractUid(principal);


        if (smartschoolUserId != null && !smartschoolUserId.isBlank()
                && !smartschoolUserId.equals(smartschoolUid)) {
            return ResponseEntity.ok(loanService.getActiveLoansAsAdmin(smartschoolUid, smartschoolUserId));
        }

        return ResponseEntity.ok(loanService.getActiveLoansByUser(smartschoolUid));
    }


    @PostMapping("/{loanId}/extension-request")
    public ResponseEntity<Void> requestLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        loanService.requestLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/extension-requests/pending")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<LoanExtensionRequestDTO>> getPendingExtensionRequests(
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        return ResponseEntity.ok(loanService.getPendingExtensionRequestsForSchool(smartschoolUid));
    }

    @PostMapping("/{loanId}/extension-request/approve")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> approveLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        loanService.approveLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{loanId}/extension-request/deny")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> denyLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = authHelper.extractUid(principal);


        loanService.denyLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/return")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> returnBooksBulk(@RequestBody List<ReturnBulkRequestDTO> requests) {
        loanService.returnBooksBulk(requests);
        return ResponseEntity.ok().build();
    }

    /** Terugbreng-endpoint voor individuele leningen. Bedoeld voor testdoeleinden. */
    @PostMapping("/{loanId}/return")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> returnBook(@PathVariable Long loanId, @RequestParam int quantity) {
        loanService.returnBook(loanId, quantity, 0, 0, 0);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<Page<LoanHistoryDTO>> getLoanHistory(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String smartschoolUid = authHelper.extractUid(principal);

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loanService.getLoanHistoryByUser(smartschoolUid, pageable));
    }

    /**
     * Geeft het aantal dagen voor de vervaldatum waarop de frontend een herinnering toont,
     * op basis van de LoanPolicy van de school van de ingelogde gebruiker.
     */
    @GetMapping("/reminder-days")
    public ResponseEntity<Integer> getReminderDays(@AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = authHelper.extractUid(principal);

        return ResponseEntity.ok(loanPolicyService.getReminderDaysForUser(smartschoolUid));
    }

    /**
     * Stuurt manueel een vervaldatumwaarschuwing naar de lener van een specifieke lening. Enkel beschikbaar voor bibliotheekbeheerders.
     */
    @PostMapping("/{loanId}/overdue-warning")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> sendOverdueWarning(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {
        String actorUid = authHelper.extractUid(principal);
        loanDueDateNotificationService.sendOverdueWarning(actorUid, loanId);
        return ResponseEntity.ok().build();
    }

}