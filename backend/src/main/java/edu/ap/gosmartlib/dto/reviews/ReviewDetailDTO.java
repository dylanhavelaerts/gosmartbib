package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.ReviewStatus;
import edu.ap.gosmartlib.util.UserRoles;

import java.time.LocalDate;

public record ReviewDetailDTO(
                Long id,
                Long userId,
                String reviewerName,
                UserRoles userRole,
                String bookISBN,
                String bookTitle,
                String text,
                LocalDate reviewDate,
                ReviewStatus reviewStatus,
                float rating,
                int flagCount) {
}