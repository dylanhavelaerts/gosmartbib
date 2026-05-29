package edu.ap.gosmartlib.controllers.loan;

import edu.ap.gosmartlib.dto.loan.LibrarianActiveLoanDTO;
import edu.ap.gosmartlib.dto.loan.LibrarianLoanHistoryDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.loans.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final AuthHelper authHelper;


    @GetMapping("/school/active")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Page<LibrarianActiveLoanDTO>> getActiveLoansForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String smartschoolUid = authHelper.extractUid(principal);

        return ResponseEntity.ok(adminLoanService.getActiveLoansForSchool(smartschoolUid, classId, page, size));
    }

    @GetMapping("/school/history")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Page<LibrarianLoanHistoryDTO>> getLoanHistoryForSchool(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String smartschoolUid = authHelper.extractUid(principal);

        return ResponseEntity.ok(adminLoanService.getLoanHistoryForSchool(smartschoolUid, classId, page, size));
    }
    @GetMapping("/school/classes")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<ReadingListAssignmentTargetsDTO.ClassTarget>> getSchoolClasses(
            @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = authHelper.extractUid(principal);

        return ResponseEntity.ok(adminLoanService.getSchoolClasses(smartschoolUid));
    }
}
