package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.dto.book.BookDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Legacy response-DTO voor een leeslijst met volledige boek-DTO's.
 *
 * <p>
 * Nieuwe leeslijstschermen gebruiken vooral ReadingListOverviewDTO en
 * ReadingListDetailDTO. Deze DTO blijft bestaan voor oudere code die nog een
 * eenvoudige leeslijstrespons verwacht.
 * </p>
 */
@Data
public class ReadingListResponseDTO {
    private Long id;
    private String title;
    private String taskDescription;
    private LocalDateTime deadline;
    private List<BookDTO> books;
}