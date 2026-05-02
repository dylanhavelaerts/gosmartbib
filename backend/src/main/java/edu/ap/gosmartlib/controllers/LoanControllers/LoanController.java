package edu.ap.gosmartlib.controllers.LoanControllers;

import edu.ap.gosmartlib.dto.loan.ActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.dto.loan.ReturnBulkRequestDTO;
import edu.ap.gosmartlib.services.Loans.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // Actieve leningen ophalen voor de frontend kolom
    @GetMapping("/active")
    public ResponseEntity<List<ActiveLoanDTO>> getActiveLoans(@RequestParam String smartschoolUserId) {
        return ResponseEntity.ok(loanService.getActiveLoansByUser(smartschoolUserId));
    }

    // Meerdere boeken in 1 keer terugbrengen via de frontend inlever-knop
    @PostMapping("/return")
    public ResponseEntity<Void> returnBooksBulk(@RequestBody List<ReturnBulkRequestDTO> requests) {
        loanService.returnBooksBulk(requests);
        return ResponseEntity.ok().build();
    }

    // Enkel boek terugbrengen (kun je behouden voor interne aanroepen / admin testing)
    @PostMapping("/{loanId}/return")
    public ResponseEntity<Void> returnBook(@PathVariable Long loanId, @RequestParam int quantity) {
        loanService.returnBook(loanId, quantity);
        return ResponseEntity.ok().build();
    }
}