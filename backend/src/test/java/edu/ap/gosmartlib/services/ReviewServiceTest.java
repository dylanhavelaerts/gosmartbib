package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewSummaryDTO;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesResponse;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReviewEntity;
import edu.ap.gosmartlib.entities.SchoolEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.AlreadyReviewedException;
import edu.ap.gosmartlib.exceptions.OutOfBoundsException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReviewRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.AutomaticModerationDecision;
import edu.ap.gosmartlib.util.ReviewStatus;
import edu.ap.gosmartlib.util.UserRoles;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserDirectoryService userDirectoryService;

    @Mock
    private ReviewAutoModerationService reviewAutoModerationService;

    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        lenient().when(reviewAutoModerationService.moderate(anyString()))
                .thenReturn(AutomaticModerationDecision.clean());
        lenient().when(reviewRepository.save(any(ReviewEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenValidRequest_whenSubmitReview_thenCreatesReviewWithDefaultState() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Strong recommendation", 4.5f, true);
        UserEntity user = buildUser(1L, "uid-1");
        BookEntity book = buildBook(10L, "9780000000001", "Domain-Driven Design");

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn("uid-1", "9780000000001")).thenReturn(false);
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any(ReviewEntity.class)))
                .thenAnswer(invocation -> {
                    ReviewEntity toSave = invocation.getArgument(0);
                    toSave.setId(99L);
                    return toSave;
                });
        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("uid-1", "Reviewer Name"),
                        List.of(),
                        "ok"));

        ReviewSummaryDTO result = reviewService.submitReview(request, "uid-1");

        assertNotNull(result);
        assertEquals(99L, result.id());
        assertEquals(1L, result.userId());
        assertEquals("Reviewer Name", result.reviewerName());
        assertEquals(UserRoles.STUDENT, result.userRole());
        assertEquals("Strong recommendation", result.text());
        assertEquals(4.5f, result.rating());
        assertEquals(true, result.spoiler());

        ArgumentCaptor<ReviewEntity> reviewCaptor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository, times(1)).save(reviewCaptor.capture());
        ReviewEntity saved = reviewCaptor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(book, saved.getBook());
        assertEquals(LocalDate.now(), saved.getReviewDate());
        assertEquals(ReviewStatus.APPROVED, saved.getReviewStatus());
        assertEquals(0, saved.getFlagCount());
        assertEquals(true, saved.isSpoiler());
    }

    @Test
    void givenNullText_whenSubmitReview_thenStoresEmptyText() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", null, 3.0f, false);
        UserEntity user = buildUser(1L, "uid-1");
        BookEntity book = buildBook(10L, "9780000000001", "Book");

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(user));
        when(bookRepository.findByIsbn("9780000000001")).thenReturn(Optional.of(book));
        when(reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn("uid-1", "9780000000001")).thenReturn(false);
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any(ReviewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("uid-1", "Reviewer Name"),
                        List.of(),
                        "ok"));

        ReviewSummaryDTO result = reviewService.submitReview(request, "uid-1");

        assertEquals("", result.text());
    }

    @Test
    void givenTooLongText_whenSubmitReview_thenThrowsOutOfBoundsException() {
        String tooLongText = "x".repeat(256);
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", tooLongText, 3.0f, false);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(buildUser(1L, "uid-1")));
        when(bookRepository.findByIsbn("9780000000001"))
                .thenReturn(Optional.of(buildBook(10L, "9780000000001", "Book")));
        when(reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn("uid-1", "9780000000001")).thenReturn(false);

        assertThrows(OutOfBoundsException.class, () -> reviewService.submitReview(request, "uid-1"));
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenInvalidRating_whenSubmitReview_thenThrowsOutOfBoundsException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Text", 6.0f, false);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(buildUser(1L, "uid-1")));
        when(bookRepository.findByIsbn("9780000000001"))
                .thenReturn(Optional.of(buildBook(10L, "9780000000001", "Book")));
        when(reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn("uid-1", "9780000000001")).thenReturn(false);

        assertThrows(OutOfBoundsException.class, () -> reviewService.submitReview(request, "uid-1"));
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenAlreadyReviewedBook_whenSubmitReview_thenThrowsAlreadyReviewedException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Text", 4.0f, false);

        when(userRepository.findBySmartschoolUid("uid-1")).thenReturn(Optional.of(buildUser(1L, "uid-1")));
        when(bookRepository.findByIsbn("9780000000001"))
                .thenReturn(Optional.of(buildBook(10L, "9780000000001", "Book")));
        when(reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn("uid-1", "9780000000001")).thenReturn(true);

        assertThrows(AlreadyReviewedException.class, () -> reviewService.submitReview(request, "uid-1"));
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenUserNotFound_whenSubmitReview_thenThrowsEntityNotFoundException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Text", 4.0f, false);
        when(userRepository.findBySmartschoolUid("uid-404")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.submitReview(request, "uid-404"));
        verify(bookRepository, never()).findByIsbn("9780000000001");
    }

    @Test
    void givenOwnerAndValidUpdate_whenEditReview_thenUpdatesAndSavesReview() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Updated review", 2.5f, true);
        ReviewEntity existing = buildReview(50L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));
        existing.setReviewStatus(ReviewStatus.APPROVED);

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(existing));

        reviewService.editReview(50L, request, "uid-1");

        assertEquals("Updated review", existing.getText());
        assertEquals(2.5f, existing.getRating());
        assertEquals(true, existing.isSpoiler());
        assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void givenNullText_whenEditReview_thenStoresEmptyText() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", null, 4.0f, false);
        ReviewEntity existing = buildReview(51L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));

        when(reviewRepository.findById(51L)).thenReturn(Optional.of(existing));

        reviewService.editReview(51L, request, "uid-1");

        assertEquals("", existing.getText());
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void givenReviewNotFound_whenEditReview_thenThrowsEntityNotFoundException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Text", 4.0f, false);
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.editReview(999L, request, "uid-1"));
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenDifferentOwner_whenEditReview_thenThrowsSecurityException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Text", 4.0f, false);
        ReviewEntity existing = buildReview(60L, buildUser(1L, "owner-uid"), buildBook(10L, "9780000000001", "Book"));

        when(reviewRepository.findById(60L)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class,
                () -> reviewService.editReview(60L, request, "other-uid"));

        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenInvalidRating_whenEditReview_thenThrowsOutOfBoundsException() {
        ReviewRequestDTO request = new ReviewRequestDTO("9780000000001", "Updated", 6.0f, false);
        ReviewEntity existing = buildReview(61L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));

        when(reviewRepository.findById(61L)).thenReturn(Optional.of(existing));

        assertThrows(OutOfBoundsException.class, () -> reviewService.editReview(61L, request, "uid-1"));
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenOwnedReview_whenUserDeleteReview_thenDeletesReview() {
        ReviewEntity existing = buildReview(70L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));
        when(reviewRepository.findById(70L)).thenReturn(Optional.of(existing));

        reviewService.userDeleteReview(70L, "uid-1", false);

        verify(reviewRepository, times(1)).delete(existing);
    }

    @Test
    void givenDifferentOwner_whenUserDeleteReview_thenThrowsSecurityException() {
        ReviewEntity existing = buildReview(71L, buildUser(1L, "owner-uid"), buildBook(10L, "9780000000001", "Book"));
        when(reviewRepository.findById(71L)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class,
                () -> reviewService.userDeleteReview(71L, "other-uid", false));
        verify(reviewRepository, never()).delete(existing);
    }

    @Test
    void givenReviewNotFound_whenUserDeleteReview_thenThrowsEntityNotFoundException() {
        when(reviewRepository.findById(72L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.userDeleteReview(72L, "uid-1", false));
        verify(reviewRepository, never()).delete(org.mockito.ArgumentMatchers.any(ReviewEntity.class));
    }

    @Test
    void givenDifferentOwnerWithModeratorDeleteAccess_whenUserDeleteReview_thenDeletesReview() {
        ReviewEntity existing = buildReview(75L, buildUser(1L, "owner-uid"), buildBook(10L, "9780000000001", "Book"));
        when(reviewRepository.findById(75L)).thenReturn(Optional.of(existing));

        reviewService.userDeleteReview(75L, "teacher-uid", true);

        verify(reviewRepository, times(1)).delete(existing);
    }

    @Test
    void givenExistingReview_whenLibrarianDeleteReview_thenDeletesReview() {
        ReviewEntity existing = buildReview(73L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));
        when(reviewRepository.findById(73L)).thenReturn(Optional.of(existing));

        reviewService.librarianDeleteReview(73L);

        verify(reviewRepository, times(1)).delete(existing);
    }

    @Test
    void givenReviewNotFound_whenLibrarianDeleteReview_thenThrowsEntityNotFoundException() {
        when(reviewRepository.findById(74L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> reviewService.librarianDeleteReview(74L));
    }

    @Test
    void givenExistingReview_whenApproveReview_thenSetsApprovedStatusAndSaves() {
        ReviewEntity existing = buildReview(80L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));
        existing.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
        when(reviewRepository.findById(80L)).thenReturn(Optional.of(existing));

        reviewService.approveReview(80L);

        assertEquals(ReviewStatus.APPROVED, existing.getReviewStatus());
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void givenExistingReview_whenRejectReview_thenSetsRejectedStatusAndSaves() {
        ReviewEntity existing = buildReview(81L, buildUser(1L, "uid-1"), buildBook(10L, "9780000000001", "Book"));
        existing.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
        when(reviewRepository.findById(81L)).thenReturn(Optional.of(existing));

        reviewService.rejectReview(81L);

        assertEquals(ReviewStatus.REJECTED, existing.getReviewStatus());
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void givenReviewId_whenIncreaseFlagCount_thenDelegatesToRepository() {
        reviewService.increaseFlagCount(90L);

        verify(reviewRepository, times(1)).incrementFlagCount(90L);
    }

    @Test
    void givenReviewForBook_whenFindAllSummaryReviewsByBook_thenMapsReviewToSummaryDTO() {
        ReviewEntity review = buildReview(91L, buildUser(7L, "uid-7"), buildBook(11L, "9780000000091", "Book 91"));
        review.setText("Compact review");
        review.setRating(3.5f);
        review.setReviewDate(LocalDate.of(2026, 2, 1));
        review.setReviewStatus(ReviewStatus.APPROVED);

        when(reviewRepository.findByBook_Isbn("9780000000091")).thenReturn(List.of(review));
        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("uid-7", "Reviewer Name"),
                        List.of(),
                        "ok"));

        List<ReviewSummaryDTO> result = reviewService.findAllSummaryReviewsByBook("9780000000091", "viewer-uid");

        assertEquals(1, result.size());
        assertEquals(91L, result.get(0).id());
        assertEquals(7L, result.get(0).userId());
        assertEquals("Reviewer Name", result.get(0).reviewerName());
        assertEquals("Compact review", result.get(0).text());
        verify(reviewRepository, times(1)).findByBook_Isbn("9780000000091");
    }

    @Test
    void givenReviewForStatus_whenFindAllReviewsByStatus_thenMapsReviewToDetailDTO() {
        ReviewEntity review = buildReview(92L, buildUser(8L, "uid-8"), buildBook(12L, "9780000000092", "Book 92"));
        review.setText("Detailed review");
        review.setRating(4.0f);
        review.setReviewDate(LocalDate.of(2026, 2, 2));
        review.setReviewStatus(ReviewStatus.APPROVED);
        review.setFlagCount(2);

        when(reviewRepository.findByReviewStatus(ReviewStatus.APPROVED)).thenReturn(List.of(review));
        when(userDirectoryService.resolveDisplayNames(any(), any()))
                .thenReturn(new ResolveDisplayNamesResponse(
                        true,
                        1,
                        1,
                        Map.of("uid-8", "Reviewer Name"),
                        List.of(),
                        "ok"));

        List<ReviewDetailDTO> result = reviewService.findAllReviewsByStatus(ReviewStatus.APPROVED, "admin-uid");

        assertEquals(1, result.size());
        ReviewDetailDTO dto = result.get(0);
        assertEquals(92L, dto.id());
        assertEquals("Reviewer Name", dto.reviewerName());
        assertEquals("9780000000092", dto.bookISBN());
        assertEquals("Book 92", dto.bookTitle());
        assertEquals(ReviewStatus.APPROVED, dto.reviewStatus());
        assertEquals(2, dto.flagCount());
        verify(reviewRepository, times(1)).findByReviewStatus(ReviewStatus.APPROVED);
    }

    private UserEntity buildUser(Long id, String smartschoolUid) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSmartschoolUid(smartschoolUid);
        user.setRole(UserRoles.STUDENT);

        SchoolEntity school = new SchoolEntity();
        school.setId(1L);
        school.setName("Test School");
        user.setSchool(school);

        return user;
    }

    private BookEntity buildBook(Long id, String isbn, String title) {
        BookEntity book = new BookEntity();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle(title);
        return book;
    }

    private ReviewEntity buildReview(Long id, UserEntity user, BookEntity book) {
        ReviewEntity review = new ReviewEntity();
        review.setId(id);
        review.setUser(user);
        review.setBook(book);
        review.setText("Original review");
        review.setRating(3.0f);
        review.setReviewDate(LocalDate.of(2026, 1, 1));
        review.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
        review.setFlagCount(0);
        return review;
    }
}
