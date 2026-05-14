package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewFlagDetailDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewRequestDTO;
import edu.ap.gosmartlib.dto.reviews.ReviewSummaryDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReviewEntity;
import edu.ap.gosmartlib.dto.userDirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.AlreadyReviewedException;
import edu.ap.gosmartlib.exceptions.OutOfBoundsException;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReviewRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import edu.ap.gosmartlib.util.AutomaticModerationDecision;
import edu.ap.gosmartlib.util.ReviewFlagReason;
import edu.ap.gosmartlib.util.ReviewStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final String FLAG_ENTRY_SEPARATOR = ";";
    private static final String FLAG_FIELD_SEPARATOR = "|";
    private static final String AUTO_MODERATION_UID = "AUTO_MODERATOR";
    private static final String AUTOMATIC_REVIEW_NOTICE = "Je review is automatisch gerapporteerd door ons systeem. Er is een melding naar de beheerder verstuurd.";

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewAutoModerationService reviewAutoModerationService;
    private final UserDirectoryService userDirectoryService;

    private final int REVIEW_MAX_CHAR_COUNT = 255;

    // region General find methods

    public List<ReviewDetailDTO> findAllReviews(String actorUid) {
        List<ReviewEntity> reviews = reviewRepository.findAll();
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, reviews);

        return reviews.stream()
                .map(review -> toDetailDTO(review, displayNames))
                .toList();
    }

    public List<ReviewDetailDTO> findAllReviewsByStatus(ReviewStatus status, String actorUid) {
        List<ReviewEntity> reviews = reviewRepository.findByReviewStatus(status);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, reviews);

        return reviews.stream()
                .map(review -> toDetailDTO(review, displayNames))
                .toList();
    }

    // Voor bij de boekDetail pagina, toont alle reviews van een boek
    public List<ReviewSummaryDTO> findAllSummaryReviewsByBook(String isbn, String actorUid) {
        List<ReviewEntity> reviews = reviewRepository.findByBook_Isbn(isbn);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, reviews);

        return reviews.stream()
                .filter(review -> !isAdminDeleted(review))
            .filter(review -> review.getReviewStatus() == ReviewStatus.APPROVED
                || (actorUid != null
                    && actorUid.equals(review.getUser().getSmartschoolUid())))
                .map(review -> toSummaryDTO(review, displayNames))
                .toList();
    }

    // Voor bij de bibliotheekbeheerder, toont alle volledige reviews met detail
    // informatie
    public List<ReviewDetailDTO> findAllDetailReviewsByBook(String isbn, String actorUid) {
        List<ReviewEntity> reviews = reviewRepository.findByBook_Isbn(isbn);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, reviews);

        return reviews.stream()
                .map(review -> toDetailDTO(review, displayNames))
                .toList();
    }

    public List<ReviewDetailDTO> findAllDetailReviewsByUserId(String smartschoolUid, String actorUid) {
        List<ReviewEntity> reviews = reviewRepository.findByUser_SmartschoolUid(smartschoolUid);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, reviews);

        return reviews.stream()
                .map(review -> toDetailDTO(review, displayNames))
                .toList();
    }

    public List<ReviewDetailDTO> findAllSchoolReviewsForModerator(String smartschoolUid) {
        UserEntity moderator = userRepository.findBySmartschoolUid(smartschoolUid)
                .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden"));

        if (moderator.getSchool() == null) {
            throw new IllegalStateException("Moderator heeft geen school gekoppeld");
        }
        List<ReviewEntity> reviews = reviewRepository.findByUser_School_Id(moderator.getSchool().getId());
        Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, reviews);

        return reviews.stream()
                .map(review -> toDetailDTO(review, displayNames))
                .toList();
    }

    // endregion

    // region Post methods
    public ReviewSummaryDTO submitReview(ReviewRequestDTO request, String smartschoolUid) {
        try {
            UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid)
                    .orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden"));

            BookEntity book = bookRepository.findByIsbn(request.bookIsbn())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Geen boek gelinkt aan ISBN: " + request.bookIsbn()));

            boolean alreadyReviewed = reviewRepository.existsByUser_SmartschoolUidAndBook_Isbn(smartschoolUid,
                    request.bookIsbn());

            if (alreadyReviewed)
                throw new AlreadyReviewedException("Je hebt dit boek al beoordeeld");

            ReviewEntity review = new ReviewEntity();
            review.setUser(user);
            review.setBook(book);

            // Tekst mag leeg zijn -> dan geef je gewoon een rating mee
            if (request.text() == null) {
                review.setText("");
            } else if (request.text().length() > REVIEW_MAX_CHAR_COUNT) {
                throw new OutOfBoundsException("Review tekst is te lang (maximaal 255 tekens)");
            } else {
                review.setText(request.text());
            }

            if (request.rating() > 5 || request.rating() < 0)
                throw new OutOfBoundsException("Rating moet tussen 0 en 5 liggen");

            review.setRating(request.rating());
            review.setSpoiler(request.spoiler());

            review.setReviewDate(LocalDate.now());
            review.setReviewStatus(ReviewStatus.APPROVED);
            review.setFlagCount(0);
            review.setAdminDeleteNote(null);
            review.setAdminDeleted(false);

            applyAutomaticModeration(review);

            ReviewEntity toSave = reviewRepository.save(review);
            refreshBookRating(book);
            Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, List.of(toSave));
            return toSummaryDTO(toSave, displayNames);

        } catch (OutOfBoundsException e) {
            throw e;
        } catch (AlreadyReviewedException e) {
            throw e;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het plaatsen van de review", e);
        }
    }

    // endregion

    // region Patch methods
    public ReviewSummaryDTO editReview(Long reviewId, ReviewRequestDTO request, String smartschoolUid) { // <--FIX: smartschoolUid toegevoegd voor ownership check
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            if (!review.getUser().getSmartschoolUid().equals(smartschoolUid)) // <--FIX: Ownership check
                throw new SecurityException("Je kan enkel je eigen reviews bewerken");

            // Tekst mag leeg zijn -> dan geef je gewoon een rating mee
            if (request.text() == null) { // <--FIX: else if zodat er geen NullPointerException gegooid wordt
                review.setText("");
            } else if (request.text().length() > REVIEW_MAX_CHAR_COUNT) {
                throw new OutOfBoundsException("Review tekst is te lang (maximaal 255 tekens)");
            } else {
                review.setText(request.text());
            }

            if (request.rating() > 5 || request.rating() < 0)
                throw new OutOfBoundsException("Rating moet tussen 0 en 5 liggen");

            review.setRating(request.rating());
            review.setSpoiler(request.spoiler());
            review.setReviewStatus(ReviewStatus.APPROVED);
            review.setAdminDeleteNote(null);
            review.setAdminDeleted(false);

            applyAutomaticModeration(review);

            ReviewEntity savedReview = reviewRepository.save(review);
            refreshBookRating(review.getBook());
            Map<String, String> displayNames = resolveDisplayNamesMap(smartschoolUid, List.of(savedReview));
            return toSummaryDTO(savedReview, displayNames);
        } catch (OutOfBoundsException e) {
            throw e;
        } catch (SecurityException e) {
            throw e;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het bewerken van de review", e);
        }
    }
    // endregion

    // region Delete methods
    public void userDeleteReview(Long reviewId, String smartschoolUid, boolean canModerateDelete) {
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            boolean ownsReview = review.getUser().getSmartschoolUid().equals(smartschoolUid);
            if (!ownsReview && !canModerateDelete)
                throw new SecurityException("Je kan enkel je eigen reviews verwijderen");

            BookEntity book = review.getBook();
            reviewRepository.delete(review);
            refreshBookRating(book);
        } catch (SecurityException e) {
            throw e;
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het verwijderen van de review", e);
        }
    }

    public void librarianDeleteReview(Long reviewId) {
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));
            BookEntity book = review.getBook();
            reviewRepository.delete(review);
            refreshBookRating(book);

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het verwijderen van de review", e);
        }
    }
    // endregion

    // region Helper methods
    public void increaseFlagCount(Long reviewId) {
        try {
            reviewRepository.incrementFlagCount(reviewId);
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het verhogen van de flag count", e);
        }
    }

    public void flagReview(Long reviewId, String smartschoolUid, ReviewFlagReason reason) {
        try {
            if (reason == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Geef een reden op voor de rapportage");
            }

            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            if (isAdminDeleted(review)) {
                throw new ResponseStatusException(BAD_REQUEST, "Deze review kan niet meer gerapporteerd worden");
            }

            boolean alreadyFlagged = reviewRepository.existsFlagByReviewIdAndUid(reviewId, smartschoolUid);
            if (alreadyFlagged) {
                throw new ResponseStatusException(CONFLICT, "Je hebt deze review al gerapporteerd");
            }

            Set<String> flaggedUids = new LinkedHashSet<>();
            String rawUids = review.getFlaggedByUids();
            if (rawUids != null && !rawUids.isBlank()) {
                flaggedUids.addAll(Arrays.stream(rawUids.split(","))
                        .map(String::trim)
                        .filter(uid -> !uid.isBlank())
                        .toList());
            }

            flaggedUids.add(smartschoolUid);
            review.setFlaggedByUids(String.join(",", flaggedUids));

            List<ReviewFlagDetailDTO> flagDetails = parseFlagDetails(review.getFlagDetails());
            flagDetails.add(new ReviewFlagDetailDTO(smartschoolUid, reason));
            review.setFlagDetails(serializeFlagDetails(flagDetails));

            review.setFlagCount(review.getFlagCount() + 1);

            if (review.getFlagCount() >= 3) {
                review.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
                review.setAdminDeleted(false);
                review.setAdminDeleteNote(null);
            }

            reviewRepository.save(review);
            refreshBookRating(review.getBook());
        } catch (Exception e) {
            if (e instanceof EntityNotFoundException || e instanceof ResponseStatusException) {
                throw e;
            }
            throw new RuntimeException("Er is iets fout gegaan tijdens het rapporteren van de review", e);
        }
    }

    public void approveReview(Long reviewId) {
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            review.setReviewStatus(ReviewStatus.APPROVED);
            review.setAdminDeleteNote(null);
            review.setAdminDeleted(false);
            reviewRepository.save(review);
            refreshBookRating(review.getBook());

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het goedkeuren van de review", e);
        }
    }

    public void rejectReview(Long reviewId) {
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            review.setReviewStatus(ReviewStatus.REJECTED);
            review.setAdminDeleteNote(null);
            review.setAdminDeleted(false);
            reviewRepository.save(review);
            refreshBookRating(review.getBook());
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het afkeuren van de review", e);
        }
    }
    public void adminDeleteReview(Long reviewId, String reason) {
        try {
            ReviewEntity review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review niet gevonden"));

            String cleanedReason = reason == null ? "" : reason.trim();

            review.setReviewStatus(ReviewStatus.REJECTED);
            review.setAdminDeleteNote(cleanedReason.isBlank() ? null : cleanedReason);
            review.setAdminDeleted(true);
            reviewRepository.save(review);
            refreshBookRating(review.getBook());
        } catch (Exception e) {
            if (e instanceof EntityNotFoundException || e instanceof ResponseStatusException) {
                throw e;
            }
            throw new RuntimeException("Er is iets fout gegaan tijdens admin delete", e);
        }
    }
    private void refreshBookRating(BookEntity book) {
        if (book == null || book.getId() == null) {
            return;
        }

        Double averageRating = reviewRepository.findAverageApprovedRatingByBookId(book.getId());
        book.setRating(averageRating != null ? averageRating : 0.0);
        bookRepository.save(book);
    }

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<ReviewEntity> reviews) {
        if (actorUid == null || actorUid.isBlank() || reviews == null || reviews.isEmpty()) {
            return Map.of();
        }

        List<String> reviewerUids = reviews.stream()
                .map(review -> review.getUser().getSmartschoolUid())
                .filter(uid -> uid != null && !uid.isBlank())
                .distinct()
                .toList();

        if (reviewerUids.isEmpty()) {
            return Map.of();
        }

        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid,
                    new ResolveDisplayNamesRequest(reviewerUids));

            if (!response.success() || response.displayNames() == null) {
                return Map.of();
            }

            return response.displayNames();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveReviewerName(ReviewEntity review, Map<String, String> displayNames) {
        String reviewerUid = review.getUser().getSmartschoolUid();
        if (reviewerUid == null || reviewerUid.isBlank()) {
            return null;
        }

        String resolvedName = displayNames.get(reviewerUid);
        if (resolvedName != null && !resolvedName.isBlank()) {
            return resolvedName;
        }

        return null;
    }
    // endregion

    // region toDTO methods
    public ReviewDetailDTO toDetailDTO(ReviewEntity review, Map<String, String> displayNames) {
        return new ReviewDetailDTO(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getSmartschoolUid(),
                resolveReviewerName(review, displayNames),
                review.getUser().getRole(),
                review.getUser().getSchool() != null ? review.getUser().getSchool().getId() : null,
                review.getUser().getSchool() != null ? review.getUser().getSchool().getName() : null,
                review.getBook().getIsbn(),
                review.getBook().getTitle(),
                review.getText(),
                review.getReviewDate(),
                review.getReviewStatus(),
                review.getRating(),
                review.getFlagCount(),
                parseFlagDetails(review.getFlagDetails()),
                isAdminDeleted(review),
                review.getAdminDeleteNote()
        );
    }

    public ReviewSummaryDTO toSummaryDTO(ReviewEntity review, Map<String, String> displayNames) {
        return new ReviewSummaryDTO(
                review.getId(),
                review.getUser().getId(),
                resolveReviewerName(review, displayNames),
                review.getUser().getRole(),
                review.getText(),
                review.getReviewDate(),
                review.getRating(),
                review.isSpoiler(),
                buildModerationNotice(review)
        );
    }

    private String buildModerationNotice(ReviewEntity review) {
        boolean autoFlagged = parseFlagDetails(review.getFlagDetails()).stream()
                .anyMatch(detail -> AUTO_MODERATION_UID.equals(detail.flaggerUid()));

        if (autoFlagged && review.getReviewStatus() == ReviewStatus.AWAITING_MODERATION) {
            return AUTOMATIC_REVIEW_NOTICE;
        }

        return null;
    }

    private List<ReviewFlagDetailDTO> parseFlagDetails(String rawFlagDetails) {
        if (rawFlagDetails == null || rawFlagDetails.isBlank()) {
            return new ArrayList<>();
        }

        List<ReviewFlagDetailDTO> parsed = new ArrayList<>();
        String[] entries = rawFlagDetails.split(FLAG_ENTRY_SEPARATOR);

        for (String entry : entries) {
            String trimmedEntry = entry.trim();
            if (trimmedEntry.isBlank()) {
                continue;
            }

            String[] fields = trimmedEntry.split("\\|", 2);
            if (fields.length != 2) {
                continue;
            }

            String uid = fields[0].trim();
            String reasonValue = fields[1].trim();
            if (uid.isBlank() || reasonValue.isBlank()) {
                continue;
            }

            try {
                ReviewFlagReason reason = ReviewFlagReason.valueOf(reasonValue);
                parsed.add(new ReviewFlagDetailDTO(uid, reason));
            } catch (IllegalArgumentException ignored) {
                // Ongeldige enum waarde / negeer
            }
        }

        return parsed;
    }

    private String serializeFlagDetails(List<ReviewFlagDetailDTO> flagDetails) {
        if (flagDetails == null || flagDetails.isEmpty()) {
            return "";
        }

        return flagDetails.stream()
                .filter(detail -> detail != null && detail.flaggerUid() != null && detail.reason() != null)
                .map(detail -> detail.flaggerUid().trim() + FLAG_FIELD_SEPARATOR + detail.reason().name())
                .collect(Collectors.joining(FLAG_ENTRY_SEPARATOR));
    }

    private void applyAutomaticModeration(ReviewEntity review) {
        AutomaticModerationDecision decision = reviewAutoModerationService.moderate(review.getText());
        if (!decision.flagged()) {
            return;
        }

        review.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
        review.setAdminDeleted(false);
        review.setAdminDeleteNote(null);

        Set<String> flaggedUids = parseFlaggedUids(review.getFlaggedByUids());
        flaggedUids.add(AUTO_MODERATION_UID);
        review.setFlaggedByUids(String.join(",", flaggedUids));

        List<ReviewFlagDetailDTO> flagDetails = parseFlagDetails(review.getFlagDetails());
        boolean hasAutomaticDetail = flagDetails.stream()
                .anyMatch(detail -> AUTO_MODERATION_UID.equals(detail.flaggerUid()));

        if (!hasAutomaticDetail) {
            flagDetails.add(new ReviewFlagDetailDTO(AUTO_MODERATION_UID, decision.reason()));
        }

        review.setFlagDetails(serializeFlagDetails(flagDetails));
        review.setFlagCount(Math.max(review.getFlagCount(), 1));
    }

    private Set<String> parseFlaggedUids(String rawUids) {
        Set<String> uids = new LinkedHashSet<>();
        if (rawUids == null || rawUids.isBlank()) {
            return uids;
        }

        Arrays.stream(rawUids.split(","))
                .map(String::trim)
                .filter(uid -> !uid.isBlank())
                .forEach(uids::add);

        return uids;
    }

    private boolean isAdminDeleted(ReviewEntity review) {
        if (review.isAdminDeleted()) {
            return true;
        }

        // Backward compatibility: existing rows before is_admin_deleted column.
        if (review.getReviewStatus() != ReviewStatus.REJECTED) {
            return false;
        }

        String note = review.getAdminDeleteNote();
        return note != null && !note.isBlank();
    }
    // endregion
}