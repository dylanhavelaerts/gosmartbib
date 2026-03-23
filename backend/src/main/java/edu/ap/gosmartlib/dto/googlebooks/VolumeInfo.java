package edu.ap.gosmartlib.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VolumeInfo {
    private String title;
    private List<String> authors;
    private String publisher;
    private String description;
    private Integer pageCount;
    private List<String> categories;
    private ImageLinks imageLinks;
    private String language;
    private Double averageRating;
    private String publishedDate;
}