package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.util.ReadingListTargetType;
import lombok.Data;
import java.util.List;

@Data
public class CreateReadingListDTO {
    private String title;
    private String taskDescription;
    private String deadline;
    private List<Long> bookIds;
    private ReadingListTargetType targetType;
    private List<Long> targetStudentIds;
    private List<Long> targetClassIds;
    private List<Integer> targetYears;
    private List<Integer> targetGrades;
    private Boolean targetAllSchools;
}