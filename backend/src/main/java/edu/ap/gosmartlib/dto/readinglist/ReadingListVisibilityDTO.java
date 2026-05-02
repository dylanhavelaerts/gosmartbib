package edu.ap.gosmartlib.dto.readinglist;

public record ReadingListVisibilityDTO(
        Long id,
        String publicUid,
        boolean publicVisible) {
}