package edu.ap.gosmartlib.dto.loan;

import edu.ap.gosmartlib.util.BookCopyCondition;

public record BookCopyReturnConditionDTO (
        Long copyId,
        String barcode,
        BookCopyCondition condition,
        String notes
) {}
