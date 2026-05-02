package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.ReadingListTargetType;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingListDetailDTO(
                Long id,
                String publicUid,
                String title,
                String taskDescription,
                LocalDateTime deadline,
                ReadingListType listType,
                boolean ownList,
                boolean publicVisible,
                String creatorName,
                ReadingListTargetType targetType,
                List<Long> targetStudentIds,
                List<String> targetStudentDisplayNames,
                List<Long> targetClassIds,
                List<String> targetClassNames,
                List<Integer> targetYears,
                List<Integer> targetGrades,
                boolean targetAllSchools,
                List<ReadingListBookDTO> books) {
}
