package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.reviews.ReviewDetailDTO;
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
import edu.ap.gosmartlib.util.ReviewFlagReason;
import edu.ap.gosmartlib.util.ReviewStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
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
        List<ReviewEntity> reviews = reviewRepository.findByStatus(status);
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
            review.setReviewStatus(ReviewStatus.AWAITING_MODERATION);
            review.setFlagCount(0);

            ReviewEntity toSave = reviewRepository.save(review);
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
    public void editReview(Long reviewId, ReviewRequestDTO request, String smartschoolUid) { // <--FIX: smartschoolUid
                                                                                             // toegevoegd voor
                                                                                             // ownership check
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
            review.setReviewStatus(ReviewStatus.AWAITING_MODERATION);

            reviewRepository.save(review);
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

            reviewRepository.delete(review);
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

            reviewRepository.delete(review);
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
            review.setFlagCount(review.getFlagCount() + 1);
            reviewRepository.save(review);
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
            reviewRepository.save(review);
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
            reviewRepository.save(review);
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Er is iets fout gegaan tijdens het afkeuren van de review", e);
        }
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

        return displayNames.get(reviewerUid);
    }
    // endregion

    // region toDTO methods
    public ReviewDetailDTO toDetailDTO(ReviewEntity review, Map<String, String> displayNames) {
        return new ReviewDetailDTO(
                review.getId(),
                review.getUser().getId(),
                resolveReviewerName(review, displayNames),
                review.getUser().getRole(),
                review.getBook().getIsbn(),
                review.getBook().getTitle(),
                review.getText(),
                review.getReviewDate(),
                review.getReviewStatus(),
                review.getRating(),
                review.getFlagCount());
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
                review.isSpoiler());
    }
    // endregion
}