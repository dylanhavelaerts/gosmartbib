package edu.ap.testbackend.controllers;

import edu.ap.testbackend.dto.loan.LoanRequestDTO;
import edu.ap.testbackend.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@CrossOrigin(origins = "*")
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