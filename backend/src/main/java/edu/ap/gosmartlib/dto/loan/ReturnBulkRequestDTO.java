package edu.ap.gosmartlib.dto.loan;

import java.util.List;

public record ReturnBulkRequestDTO(
        Long bookId,
        int quantity,
        String smartschoolUserId,
        List<BookCopyReturnConditionDTO> copyConditions,
        int damagedCount,
        int brokenCount,
        int lostCount
) {}