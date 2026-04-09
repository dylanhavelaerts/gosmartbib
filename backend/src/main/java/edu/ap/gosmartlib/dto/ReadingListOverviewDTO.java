package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.util.ReadingListType;
import java.time.LocalDateTime;
import java.util.List;

public record ReadingListOverviewDTO(
        Long id,
        String title,
        String taskDescription,
        LocalDateTime deadline,
        ReadingListType listType,
        boolean ownList,
        String creatorName,
        List<Long> bookIds,
        int bookCount
) {}
