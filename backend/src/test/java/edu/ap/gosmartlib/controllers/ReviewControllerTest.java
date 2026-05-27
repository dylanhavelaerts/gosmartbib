package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.reviews.*;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.ReviewService;
import edu.ap.gosmartlib.util.ReviewFlagReason;
import edu.ap.gosmartlib.util.ReviewStatus;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private OAuth2User principal;

    @Mock
    private Authentication authentication;

    @Test
    void givenReviewsForBookExist_whenGetReviewsByBook_thenReturnsOkWithBody() {
        when(principal.getAttribute("userID")).thenReturn("viewer-uid");
        List<ReviewSummaryDTO> expected = List.of(buildSummary(1L, "Reviewer Name", "Great read", 4.5f));
        when(reviewService.findAllSummaryReviewsByBook("9780000000001", "viewer-uid")).thenReturn(expected);

        ResponseEntity<List<ReviewSummaryDTO>> response = reviewController.getReviewsByBook("9780000000001", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllSummaryReviewsByBook("9780000000001", "viewer-uid");
    }

    @Test
    void givenNoPrincipal_whenGetReviewsByBook_thenReturnsOkWithBodyAndUsesNullActorUid() {
        List<ReviewSummaryDTO> expected = List.of(
                buildSummary(1L, null, "Great read", 4.5f));
        when(reviewService.findAllSummaryReviewsByBook("9780000000001", null))
                .thenReturn(expected);

        ResponseEntity<List<ReviewSummaryDTO>> response = reviewController.getReviewsByBook("9780000000001", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1))
                .findAllSummaryReviewsByBook("9780000000001", null);
    }

    @Test
    void givenReviewsForUserExist_whenGetReviewsByUser_thenReturnsOkWithBody() {
        when(principal.getAttribute("userID")).thenReturn("admin-uid");
        List<ReviewDetailDTO> expected = List
            .of(buildDetail(2L, "Reviewer Name", "9780000000002", "Book B", ReviewStatus.APPROVED));
        when(reviewService.findAllDetailReviewsByUserId("u-123", "admin-uid")).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getReviewsByUser("u-123", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllDetailReviewsByUserId("u-123", "admin-uid");
    }

    @Test
    void givenStatusFilter_whenGetReviewsByStatus_thenReturnsOkWithBody() {
        when(principal.getAttribute("userID")).thenReturn("admin-uid");
        List<ReviewDetailDTO> expected = List
            .of(buildDetail(3L, "Reviewer Name", "9780000000003", "Book C", ReviewStatus.REJECTED));
        when(reviewService.findAllReviewsByStatus(ReviewStatus.REJECTED, "admin-uid")).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getReviewsByStatus(ReviewStatus.REJECTED,
                principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllReviewsByStatus(ReviewStatus.REJECTED, "admin-uid");
    }

    @Test
    void givenReviewsExist_whenGetAllReviews_thenReturnsOkWithBody() {
        when(principal.getAttribute("userID")).thenReturn("admin-uid");
        List<ReviewDetailDTO> expected = List
                .of(buildDetail(4L, "Reviewer Name", "9780000000004", "Book D", ReviewStatus.AWAITING_MODERATION));
        when(reviewService.findAllReviews("admin-uid")).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getAllReviews(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).findAllReviews("admin-uid");
    }

    @Test
    void givenValidPrincipal_whenSubmitReview_thenReturnsCreatedAndDelegatesToService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f, false, false);
        ReviewSummaryDTO expected = buildSummary(5L, "Reviewer Name", "Nice", 4.0f);
        when(principal.getAttribute("userID")).thenReturn("smart-uid-1");
        when(reviewService.submitReview(request, "smart-uid-1")).thenReturn(expected);

        ResponseEntity<ReviewSummaryDTO> response = reviewController.submitReview(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).submitReview(request, "smart-uid-1");
    }

    @Test
    void givenMissingPrincipal_whenSubmitReview_thenThrowsUnauthorizedAndDoesNotCallService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f, false, false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.submitReview(request, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void givenPrincipalWithoutUserId_whenSubmitReview_thenThrowsUnauthorizedAndDoesNotCallService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000005", "Nice", 4.0f, false, false);
        when(principal.getAttribute("userID")).thenReturn(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.submitReview(request, principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(reviewService, never()).submitReview(request, "");
    }

    @Test
    void givenValidPrincipal_whenEditReview_thenReturnsOkAndDelegatesToService() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000006", "Updated text", 3.0f, false, false);
        ReviewSummaryDTO expected = buildSummary(42L, null, "Updated text", 3.0f);
        when(principal.getAttribute("userID")).thenReturn("smart-uid-2");
        when(reviewService.editReview(42L, request, "smart-uid-2")).thenReturn(expected);

        ResponseEntity<ReviewSummaryDTO> response = reviewController.editReview(42L, request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService, times(1)).editReview(42L, request, "smart-uid-2");
    }

    @Test
    void givenInvalidPrincipal_whenUserDeleteReview_thenThrowsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.userDeleteReview(10L, principal, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(reviewService, never()).userDeleteReview(10L, null, false);
    }

    @Test
    void givenValidPrincipal_whenUserDeleteReview_thenReturnsNoContent() {
        when(principal.getAttribute("userID")).thenReturn("smart-uid-3");
        doReturn(List.<SimpleGrantedAuthority>of()).when(authentication).getAuthorities();

        ResponseEntity<Void> response = reviewController.userDeleteReview(10L, principal, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).userDeleteReview(10L, "smart-uid-3", false);
    }

    @Test
    void givenTeacherPrincipal_whenUserDeleteReview_thenDelegatesWithoutModeratorDeleteAccess() {
        when(principal.getAttribute("userID")).thenReturn("teacher-uid");
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TEACHER"));
        doReturn(authorities).when(authentication).getAuthorities();

        ResponseEntity<Void> response = reviewController.userDeleteReview(11L, principal, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).userDeleteReview(11L, "teacher-uid", false);
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
        String testUid = "test-uid";
        ReviewFlagRequestDTO request = new ReviewFlagRequestDTO(ReviewFlagReason.SPAM);
        when(principal.getAttribute("userID")).thenReturn(testUid);

        ResponseEntity<Void> response = reviewController.flagReview(102L, request, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).flagReview(102L, testUid, ReviewFlagReason.SPAM);
    }

    @Test
    void givenReviewId_whenLibrarianDeleteReview_thenReturnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = reviewController.librarianDeleteReview(103L);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService, times(1)).librarianDeleteReview(103L);
    }

    @Test
    void givenModeratorPrincipal_whenGetModerationReviews_thenReturnsOkWithBody() {
        when(principal.getAttribute("userID")).thenReturn("moderator-uid");
        List<ReviewDetailDTO> expected = List.of(buildDetail(10L, "Student", "97801", "Book X", ReviewStatus.AWAITING_MODERATION));
        when(reviewService.findAllSchoolReviewsForModerator("moderator-uid")).thenReturn(expected);

        ResponseEntity<List<ReviewDetailDTO>> response = reviewController.getModerationReviews(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(reviewService).findAllSchoolReviewsForModerator("moderator-uid");
    }

    @Test
    void givenReviewIdAndReason_whenAdminDeleteReview_thenReturnsNoContent() {
        AdminDeleteReviewRequestDTO request = new AdminDeleteReviewRequestDTO("Ongepaste taal");

        ResponseEntity<Void> response = reviewController.adminDeleteReview(50L, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService).adminDeleteReview(50L, "Ongepaste taal");
    }

    @Test
    void givenLibrarianPrincipal_whenUserDeleteReview_thenDelegatesWithModeratorDeleteAccess() {
        when(principal.getAttribute("userID")).thenReturn("bib-uid");
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        doReturn(authorities).when(authentication).getAuthorities();

        ResponseEntity<Void> response = reviewController.userDeleteReview(12L, principal, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService).userDeleteReview(12L, "bib-uid", true);
    }

    @Test
    void givenNullAuthentication_whenUserDeleteReview_thenCallsServiceWithFalseModeratorAccess() {
        when(principal.getAttribute("userID")).thenReturn("user-uid");

        ResponseEntity<Void> response = reviewController.userDeleteReview(13L, principal, null);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService).userDeleteReview(13L, "user-uid", false);
    }

    @Test
    void givenPrincipalWithEmptyUserId_whenExtractUid_thenThrowsUnauthorized() {
        when(principal.getAttribute("userID")).thenReturn("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reviewController.getAllReviews(principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Geen geldige gebruiker", exception.getReason());
    }

    @Test
    void givenNullPrincipal_whenExtractUidOrNull_thenReturnsNull() {
        ResponseEntity<List<ReviewSummaryDTO>> response = reviewController.getReviewsByBook("97801", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reviewService).findAllSummaryReviewsByBook("97801", null);
    }

    @Test
    void givenAuthenticationWithoutAuthorities_whenUserDeleteReview_thenCallsServiceWithFalseModeratorAccess() {
        when(principal.getAttribute("userID")).thenReturn("user-uid");
        when(authentication.getAuthorities()).thenReturn(null);

        ResponseEntity<Void> response = reviewController.userDeleteReview(14L, principal, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reviewService).userDeleteReview(14L, "user-uid", false);
    }

    private ReviewSummaryDTO buildSummary(Long id, String reviewerName, String text, float rating) {
        return new ReviewSummaryDTO(id, 11L, reviewerName, UserRoles.STUDENT, text, LocalDate.of(2026, 1, 1), rating,
                false, null);
    }

    private ReviewDetailDTO buildDetail(Long id, String reviewerName, String isbn, String bookTitle,
            ReviewStatus status) {
        return new ReviewDetailDTO(
                id,
                11L,
                "reviewer-uid",
                reviewerName,
                UserRoles.STUDENT,
                1L,
                "Test School",
                isbn,
                bookTitle,
                "Review text",
                LocalDate.of(2026, 1, 1),
                status,
                4.0f,
                0,
                List.of(),
                false,
                null);
    }
}
