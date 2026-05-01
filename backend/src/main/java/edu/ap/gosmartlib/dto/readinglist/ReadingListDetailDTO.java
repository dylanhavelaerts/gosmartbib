package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.util.ReadingListType;
import edu.ap.gosmartlib.util.ReadingListTargetType;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingListDetailDTO(
                Long id,
                String title,
                String taskDescription,
                LocalDateTime deadline,
                ReadingListType listType,
                boolean ownList,
                String creatorName,
                ReadingListTargetType targetType,
                List<Long> tagetStudentIds,
                List<String> tagetStudentDisplayNames,
                List<Long> targetClassIds,
                List<String> targetClassNames,
                List<Integer> targetYears,
                List<Integer> targetGrades,
                boolean targetAllSchools,
                List<BookItem> books) {
        public record BookItem(
                        Long id,
                        String title,
                        List<String> authors,
                        String thumbnail,
                        String isbn,
                        int availableCopies) {
        }
}
