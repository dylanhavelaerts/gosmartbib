package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.UserRoles;

import java.time.LocalDate;

public record ReviewSummaryDTO(
        Long userId, // Nog nadenken hoe met naam (GDRP)
        UserRoles userRole,
        String text,
        LocalDate reviewDate,
        int rating
) {
}
