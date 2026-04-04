package edu.ap.gosmartlib.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateReadingListDTO {
    private String title;
    private String taskDescription;
    private String deadline;
    private List<Long> bookIds;
}