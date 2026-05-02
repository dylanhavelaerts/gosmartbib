package edu.ap.gosmartlib.dto.readinglist;

import java.time.LocalDateTime;
import java.util.List;

public record PublicReadingListDetailDTO(
        String publicUid,
        String title,
        String taskDescription,
        LocalDateTime deadline,
        String creatorRole,
        List<ReadingListBookDTO> books) {
}