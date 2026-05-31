package edu.ap.gosmartlib.dto.readinglist;

/**
 * Payload om de publieke zichtbaarheid van een persoonlijke leeslijst te
 * wijzigen.
 */
public record UpdateReadingListVisibilityDTO(
                boolean publicVisible) {
}