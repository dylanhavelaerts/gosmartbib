package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.util.ReadingListType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReadingListOverviewDTO {
    private Long id;
    private String title;
    private String taskDescription;
    private LocalDateTime deadline;
    private ReadingListType listType;
    private boolean archived;
    private boolean ownList;
    private String creatorName;
    private List<Long> bookIds;
    private int bookCount;
}
