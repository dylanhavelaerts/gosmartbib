package edu.ap.gosmartlib.controllers.loanControllers;

import edu.ap.gosmartlib.dto.loan.LibrarianActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LibrarianLoanHistoryDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.services.Loans.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<Page<LibrarianActiveLoanDTO>> getActiveLoansForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (principal == null || principal.getAttribute("userID") == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String actorUid = principal.getAttribute("userID");
        return ResponseEntity.ok(adminLoanService.getActiveLoansForSchool(actorUid, classId, page, size));
    }

    @GetMapping("/school/history")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<Page<LibrarianLoanHistoryDTO>> getLoanHistoryForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (principal == null || principal.getAttribute("userID") == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String actorUid = principal.getAttribute("userID");
        return ResponseEntity.ok(adminLoanService.getLoanHistoryForSchool(actorUid, classId, page, size));
    }
    @GetMapping("/school/classes")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<List<ReadingListAssignmentTargetsDTO.ClassTarget>> getSchoolClasses(
            @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null || principal.getAttribute("userID") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String actorUid = principal.getAttribute("userID");
        return ResponseEntity.ok(adminLoanService.getSchoolClasses(actorUid));
    }
}
