package edu.ap.gosmartlib.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private List<GoogleBookItem> items;
}