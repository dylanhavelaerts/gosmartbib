package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewSummaryDTO;
import edu.ap.gosmartlib.services.ReviewService;
import edu.ap.gosmartlib.util.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/book/{isbn}")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewsByBook(@PathVariable String isbn) {
        return ResponseEntity.ok(reviewService.findAllSummaryReviewsByBook(isbn));
    }

    @GetMapping("/user/{smartschoolUid}")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<List<ReviewDetailDTO>> getReviewsByUser(@PathVariable String smartschoolUid) {
        return ResponseEntity.ok(reviewService.findAllDetailReviewsByUserId(smartschoolUid));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<List<ReviewDetailDTO>> getReviewsByStatus(@PathVariable ReviewStatus status) {
        return ResponseEntity.ok(reviewService.findAllReviewsByStatus(status));
    }

    @GetMapping
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<List<ReviewDetailDTO>> getAllReviews() {
        return ResponseEntity.ok(reviewService.findAllReviews());
    }

    @PostMapping
    public ResponseEntity<ReviewSummaryDTO> submitReview(@RequestBody ReviewRequestDTO request, @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = extractUid(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submitReview(request, smartschoolUid));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<Void> editReview(@PathVariable Long reviewId,  @RequestBody ReviewRequestDTO request, @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = extractUid(principal);
        reviewService.editReview(reviewId, request, smartschoolUid);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<Void> approveReview(@PathVariable Long reviewId) {
        reviewService.approveReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<Void> rejectReview(@PathVariable Long reviewId) {
        reviewService.rejectReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{reviewId}/flag")
    public ResponseEntity<Void> flagReview(@PathVariable Long reviewId) {
        reviewService.increaseFlagCount(reviewId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> userDeleteReview(@PathVariable Long reviewId, @AuthenticationPrincipal OAuth2User principal) {
        String smartschoolUid = extractUid(principal);
        reviewService.userDeleteReview(reviewId, smartschoolUid);
        return ResponseEntity.noContent().build();
    }

    private String extractUid(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");
        }

        String uid = principal.getAttribute("userID");
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");
        }

        return uid;
    }

    @DeleteMapping("/{reviewId}/librarian")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<Void> librarianDeleteReview(@PathVariable Long reviewId) {
        reviewService.librarianDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
