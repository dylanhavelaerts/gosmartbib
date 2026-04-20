package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.ReviewStatus;
import edu.ap.gosmartlib.util.UserRoles;

import java.time.LocalDate;
import java.util.List;

public record ReviewDetailDTO(
        Long id,
        Long userId,
        String userSmartschoolUid,
        UserRoles userRole,
        Long schoolId,
        String schoolName,
        String bookISBN,
        String bookTitle,
        String text,
        LocalDate reviewDate,
        ReviewStatus reviewStatus,
        float rating,
        int flagCount,
        List<ReviewFlagDetailDTO> flagDetails,
        boolean adminDeleted,
        String adminDeleteNote
) {}