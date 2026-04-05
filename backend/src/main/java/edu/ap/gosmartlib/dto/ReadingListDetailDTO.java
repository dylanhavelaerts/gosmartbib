package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.util.ReadingListType;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingListDetailDTO(
        Long id,
        String title,
        String taskDescription,
        LocalDateTime deadline,
        ReadingListType listType,
        boolean archived,
        boolean ownList,
        String creatorName,
        List<BookItem> books
) {
    public record BookItem(
            Long id,
            String title,
            List<String> authors,
            String thumbnail,
            String isbn
    ) {}
}
