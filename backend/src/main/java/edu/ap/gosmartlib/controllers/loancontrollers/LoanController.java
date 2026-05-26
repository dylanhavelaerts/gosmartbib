package edu.ap.gosmartlib.controllers.loanControllers;

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

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;
    private final LoanPolicyService loanPolicyService;
    private final AuthHelper authHelper;
    private final LoanDueDateNotificationService loanDueDateNotificationService;



    // Bestaande functie: Boeken uitlenen
    @PostMapping
    public ResponseEntity<Void> createLoans(@RequestBody List<LoanRequestDTO> requests) {
        loanService.createLoans(requests);
        return ResponseEntity.ok().build();
    }

    // AANGEPAST: Actieve leningen ophalen voor de frontend kolom (Veilig via
    // sessie)
    @GetMapping("/active")
    public ResponseEntity<List<ActiveLoanDTO>> getActiveLoans(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String smartschoolUserId) {

        String smartschoolUid = authHelper.extractUid(principal);


        // Als er een andere gebruiker wordt opgevraagd, controleer dan of de ingelogde
        // gebruiker een bibliotheekbeheerder is
        if (smartschoolUserId != null && !smartschoolUserId.isBlank()
                && !smartschoolUserId.equals(smartschoolUid)) {
            return ResponseEntity.ok(loanService.getActiveLoansAsAdmin(smartschoolUid, smartschoolUserId));
        }

        return ResponseEntity.ok(loanService.getActiveLoansByUser(smartschoolUid));
    }


    // Student/leerkracht vraagt verlenging aan voor eigen lening
    @PostMapping("/{loanId}/extension-request")
    public ResponseEntity<Void> requestLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        loanService.requestLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    // Bibliotheekbeheerder haalt open aanvragen op van eigen school
    @GetMapping("/extension-requests/pending")
    public ResponseEntity<List<LoanExtensionRequestDTO>> getPendingExtensionRequests(
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        return ResponseEntity.ok(loanService.getPendingExtensionRequestsForSchool(smartschoolUid));
    }

    // Bibliotheekbeheerder keurt verlenging goed
    @PostMapping("/{loanId}/extension-request/approve")
    public ResponseEntity<Void> approveLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        String smartschoolUid = authHelper.extractUid(principal);


        loanService.approveLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    // Bibliotheekbeheerder weigert verlenging
    @PostMapping("/{loanId}/extension-request/deny")
    public ResponseEntity<Void> denyLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = authHelper.extractUid(principal);


        loanService.denyLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    // Meerdere boeken in 1 keer terugbrengen via de frontend inlever-knop
    @PostMapping("/return")
    public ResponseEntity<Void> returnBooksBulk(@RequestBody List<ReturnBulkRequestDTO> requests) {
        loanService.returnBooksBulk(requests);
        return ResponseEntity.ok().build();
    }

    // Enkel boek terugbrengen (kun je behouden voor interne aanroepen / admin
    // testing)
    @PostMapping("/{loanId}/return")
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

    @GetMapping("/reminder-days")
    public ResponseEntity<Integer> getReminderDays(@AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = authHelper.extractUid(principal);

        return ResponseEntity.ok(loanPolicyService.getReminderDaysForUser(smartschoolUid));
    }
    @PostMapping("/{loanId}/overdue-warning")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<Void> sendOverdueWarning(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {
        String actorUid = authHelper.extractUid(principal);
        loanDueDateNotificationService.sendOverdueWarning(actorUid, loanId);
        return ResponseEntity.ok().build();
    }

}