package edu.ap.gosmartlib.dto.readinglist;

/**
 * Respons na het aanpassen van de publieke zichtbaarheid van een persoonlijke
 * leeslijst.
 */
public record ReadingListVisibilityDTO(
                Long id,
                String publicUid,
                boolean publicVisible) {
}