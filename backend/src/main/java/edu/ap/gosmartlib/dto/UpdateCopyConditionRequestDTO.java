package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.util.BookCopyCondition;

public record UpdateCopyConditionRequestDTO(
        BookCopyCondition condition,
        String notes
) {}
