package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.dto.book.BookDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingListResponseDTO {
    private Long id;
    private String title;
    private String taskDescription;
    private LocalDateTime deadline;
    private List<BookDTO> books;
}