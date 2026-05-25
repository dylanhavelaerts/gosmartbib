package edu.ap.gosmartlib.dto.lessontip;

import java.time.LocalDate;

public record LessonTipDTO(
        Long id,
        String text,
        boolean anonymous,
        String authorName,
        boolean ownTip,
        LocalDate createdDate
) {}
