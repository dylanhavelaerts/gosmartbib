package edu.ap.gosmartlib.dto.school;

public record HomepageSettingsDTO(
    Long schoolId,
    boolean showSpotlight,
    boolean showNewInLibrary,
    boolean showReadingLists,
    boolean showUrgentLoans
) {}