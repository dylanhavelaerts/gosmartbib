package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.util.ReadingListTargetType;
import edu.ap.gosmartlib.util.ReadingListType;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingListOverviewDTO(
        Long id,
        String publicUid,
        String title,
        String taskDescription,
        LocalDateTime deadline,
        String creatorName,
        int bookCount,
        List<Long> bookIds,
        ReadingListType listType,
        boolean ownList,
        boolean publicVisible,
        ReadingListTargetType targetType,
        List<String> targetStudentDisplayNames,
        List<String> targetClassNames,
        List<Integer> targetYears,
        List<Integer> targetGrades,
        Boolean targetAllSchools) {
}
