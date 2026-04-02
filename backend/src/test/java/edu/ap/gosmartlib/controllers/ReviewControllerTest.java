package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewSummaryDTO;
import edu.ap.gosmartlib.services.ReviewService;
import edu.ap.gosmartlib.util.ReviewStatus;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private OAuth2User principal;

    @Test
    void givenReviewsForBookExist_whenGetReviewsByBook_thenReturnsOkWithBody() {
        List<ReviewSummaryDTO> expected = List.of(buildSummary(1L, "Great read", 4.5f));
        when(reviewService.findAllSummaryReviewsByBook("9780000000001")).thenReturn(expected);

        ResponseEntity<List<ReviewSummaryDTO>> response = reviewController.getReviewsByBook("9780000000001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllSummaryReviewsByBook("9780000000001");
    }

    @Test
    void givenReviewsForUserExist_whenGetReviewsByUser_thenReturnsOkWithBody() {
        List<ReviewDetailDTO> expected = List.of(buildDetail(2L, "9780000000002", "Book B", ReviewStatus.APPROVED));
        when(reviewService.findAllDetailReviewsByUserId("u-123")).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getReviewsByUser("u-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllDetailReviewsByUserId("u-123");
    }

    @Test
    void givenStatusFilter_whenGetReviewsByStatus_thenReturnsOkWithBody() {
        List<ReviewDetailDTO> expected = List.of(buildDetail(3L, "9780000000003", "Book C", ReviewStatus.REJECTED));
        when(reviewService.findAllReviewsByStatus(ReviewStatus.REJECTED)).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getReviewsByStatus(ReviewStatus.REJECTED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllReviewsByStatus(ReviewStatus.REJECTED);
    }

    @Test
    void givenReviewsExist_whenGetAllReviews_thenReturnsOkWithBody() {
        List<ReviewDetailDTO> expected = List.of(buildDetail(4L, "9780000000004", "Book D", ReviewStatus.AWAITING_MODERATION));
        when(reviewService.findAllReviews()).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getAllReviews();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllReviews();
    }

    @Test
    void givenValidPrincipal_whenSubmitReview_thenReturnsCreatedAndDelegatesToService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f);
        ReviewSummaryDTO expected = buildSummary(5L, "Nice", 4.0f);
        when(principal.getAttribute("userID")).thenReturn("smart-uid-1");
        when(reviewService.submitReview(request, "smart-uid-1")).thenReturn(expected);

        ResponseEntity<ReviewSummaryDTO> response = reviewController.submitReview(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).submitReview(request, "smart-uid-1");
    }

    @Test
    void givenMissingPrincipal_whenSubmitReview_thenThrowsUnauthorizedAndDoesNotCallService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.submitReview(request, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void givenPrincipalWithoutUserId_whenSubmitReview_thenThrowsUnauthorizedAndDoesNotCallService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f);
        when(principal.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.submitReview(request, principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(reviewService, never()).submitReview(request, "");
    }

    @Test
    void givenValidPrincipal_whenEditReview_thenReturnsNoContentAndDelegatesToService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000006", "Updated text", 3.0f);
        when(principal.getAttribute("userID")).thenReturn("smart-uid-2");

        ResponseEntity<Void> response = reviewController.editReview(42L, request, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).editReview(42L, request, "smart-uid-2");
    }

    @Test
    void givenInvalidPrincipal_whenUserDeleteReview_thenThrowsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.userDeleteReview(10L, principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(reviewService, never()).userDeleteReview(10L, null);
    }

    @Test
    void givenValidPrincipal_whenUserDeleteReview_thenReturnsNoContent() {
        when(principal.getAttribute("userID")).thenReturn("smart-uid-3");

        ResponseEntity<Void> response = reviewController.userDeleteReview(10L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).userDeleteReview(10L, "smart-uid-3");
    }

    @Test
    void givenReviewId_whenApproveReview_thenReturnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = reviewController.approveReview(100L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).approveReview(100L);
    }

    @Test
    void givenReviewId_whenRejectReview_thenReturnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = reviewController.rejectReview(101L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).rejectReview(101L);
    }

    @Test
    void givenReviewId_whenFlagReview_thenReturnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = reviewController.flagReview(102L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).increaseFlagCount(102L);
    }

    @Test
    void givenReviewId_whenLibrarianDeleteReview_thenReturnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = reviewController.librarianDeleteReview(103L);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).librarianDeleteReview(103L);
    }

    private ReviewSummaryDTO buildSummary(Long id, String text, float rating) {
        return new ReviewSummaryDTO(id, 11L, UserRoles.STUDENT, text, LocalDate.of(2026, 1, 1), rating);
    }

    private ReviewDetailDTO buildDetail(Long id, String isbn, String bookTitle, ReviewStatus status) {
        return new ReviewDetailDTO(
                id,
                11L,
                UserRoles.STUDENT,
                isbn,
                bookTitle,
                "Review text",
                LocalDate.of(2026, 1, 1),
                status,
                4.0f,
                0);
    }
}

