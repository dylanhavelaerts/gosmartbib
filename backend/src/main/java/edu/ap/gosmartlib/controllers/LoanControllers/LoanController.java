package edu.ap.gosmartlib.controllers.LoanControllers;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanExtensionRequestDTO;
import edu.ap.gosmartlib.dto.loan.LoanHistoryDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.services.Loans.LoanService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

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

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInUid = (String) principal.getAttribute("userID");

        // Als er een andere gebruiker wordt opgevraagd, controleer dan of de ingelogde
        // gebruiker een bibliotheekbeheerder is
        if (smartschoolUserId != null && !smartschoolUserId.isBlank()
                && !smartschoolUserId.equals(loggedInUid)) {
            return ResponseEntity.ok(loanService.getActiveLoansAsAdmin(loggedInUid, smartschoolUserId));
        }

        return ResponseEntity.ok(loanService.getActiveLoansByUser(loggedInUid));
    }


    // Student/leerkracht vraagt verlenging aan voor eigen lening
    @PostMapping("/{loanId}/extension-request")
    public ResponseEntity<Void> requestLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String smartschoolUid = principal.getAttribute("userID");

        loanService.requestLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    // Bibliotheekbeheerder haalt open aanvragen op van eigen school
    @GetMapping("/extension-requests/pending")
    public ResponseEntity<List<LoanExtensionRequestDTO>> getPendingExtensionRequests(
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String smartschoolUid = principal.getAttribute("userID");

        return ResponseEntity.ok(loanService.getPendingExtensionRequestsForSchool(smartschoolUid));
    }

    // Bibliotheekbeheerder keurt verlenging goed
    @PostMapping("/{loanId}/extension-request/approve")
    public ResponseEntity<Void> approveLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String smartschoolUid = principal.getAttribute("userID");

        loanService.approveLoanExtension(loanId, smartschoolUid);
        return ResponseEntity.ok().build();
    }

    // Bibliotheekbeheerder weigert verlenging
    @PostMapping("/{loanId}/extension-request/deny")
    public ResponseEntity<Void> denyLoanExtension(
            @PathVariable Long loanId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String smartschoolUid = principal.getAttribute("userID");

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
        loanService.returnBook(loanId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<LoanHistoryDTO>> getLoanHistory(@AuthenticationPrincipal OAuth2User principal) {
        // Controleer of de gebruiker is ingelogd
        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Haal het Smartschool UID op uit de sessie
        String smartschoolUid = principal.getAttribute("userID");

        // Haal data op via service
        List<LoanHistoryDTO> history = loanService.getLoanHistoryByUser(smartschoolUid);
        return ResponseEntity.ok(history);
    }
}