package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.util.BookCopyCondition;

public record UpdateCopyConditionRequest(
        BookCopyCondition condition,
        String notes
) {}
