package edu.ap.gosmartlib.dto.school;

public record SchoolLibrarySettingsDTO(
        Long schoolId,
        boolean barcodesEnabled
) {}
