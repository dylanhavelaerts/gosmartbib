package edu.ap.gosmartlib.dto.readinglist;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailweergave van een publiek gedeelde persoonlijke leeslijst.
 */
public record PublicReadingListDetailDTO(
                String publicUid,
                String title,
                String taskDescription,
                LocalDateTime deadline,
                String creatorRole,
                List<ReadingListBookDTO> books) {
}