package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.loan.LoanRequestDTO;
import edu.ap.gosmartlib.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<Void> createLoans(@RequestBody List<LoanRequestDTO> requests) {
        loanService.createLoans(requests);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{loanId}/return")
    public ResponseEntity<Void> returnBook(@PathVariable Long loanId, @RequestParam int quantity) {
        loanService.returnBook(loanId, quantity);
        return ResponseEntity.ok().build();
    }
}