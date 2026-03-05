package edu.ap.testbackend.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBookItem {
    private VolumeInfo volumeInfo;
}