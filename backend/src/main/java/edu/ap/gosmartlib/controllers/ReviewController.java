package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
import edu.ap.gosmartlib.dto.reviews.AdminDeleteReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewFlagRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewSummaryDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.security.RoleGuard;
import edu.ap.gosmartlib.services.ReviewService;
import edu.ap.gosmartlib.util.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthHelper authHelper;
    private final RoleGuard roleGuard;

    @GetMapping("/book/{isbn}")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewsByBook(@PathVariable String isbn,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.findAllSummaryReviewsByBook(isbn, authHelper.extractUidOrNull(principal)));
    }

    @GetMapping("/user/{smartschoolUid}")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<ReviewDetailDTO>> getReviewsByUser(@PathVariable String smartschoolUid,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.findAllDetailReviewsByUserId(smartschoolUid, authHelper.extractUid(principal)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<ReviewDetailDTO>> getReviewsByStatus(@PathVariable ReviewStatus status,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.findAllReviewsByStatus(status, authHelper.extractUid(principal)));
    }

    @GetMapping
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<ReviewDetailDTO>> getAllReviews(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.findAllReviews(authHelper.extractUid(principal)));
    }

    @GetMapping("/moderation")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<List<ReviewDetailDTO>> getModerationReviews(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.findAllSchoolReviewsForModerator(authHelper.extractUid(principal)));
    }

    @PostMapping
    public ResponseEntity<ReviewSummaryDTO> submitReview(@RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submitReview(request, authHelper.extractUid(principal)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewSummaryDTO> editReview(@PathVariable Long reviewId, @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(reviewService.editReview(reviewId, request, authHelper.extractUid(principal)));
    }

    @PatchMapping("/{reviewId}/approve")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> approveReview(@PathVariable Long reviewId) {
        reviewService.approveReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reject")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> rejectReview(@PathVariable Long reviewId) {
        reviewService.rejectReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/admin-delete")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> adminDeleteReview(@PathVariable Long reviewId,
                                                  @RequestBody AdminDeleteReviewRequestDTO request) {
        reviewService.adminDeleteReview(reviewId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/flag")
    public ResponseEntity<Void> flagReview(@PathVariable Long reviewId, @RequestBody ReviewFlagRequestDTO request,
            @AuthenticationPrincipal OAuth2User principal) {
        reviewService.flagReview(reviewId, authHelper.extractUid(principal), request.reason());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> userDeleteReview(@PathVariable Long reviewId,
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        reviewService.userDeleteReview(reviewId, authHelper.extractUid(principal), roleGuard.isLibrarian(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reviewId}/librarian")
    @PreAuthorize("@roleGuard.isLibrarian(authentication)")
    public ResponseEntity<Void> librarianDeleteReview(@PathVariable Long reviewId) {
        reviewService.librarianDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
