package edu.ap.gosmartlib.dto.reviews;

import edu.ap.gosmartlib.util.UserRoles;

import java.time.LocalDate;

public record ReviewSummaryDTO(
        Long id,
        Long userId, // Nog nadenken hoe met naam (GDRP)
        UserRoles userRole,
        String text,
        LocalDate reviewDate,
        float rating,
        boolean spoiler,
        String moderationNotice
) {
}
