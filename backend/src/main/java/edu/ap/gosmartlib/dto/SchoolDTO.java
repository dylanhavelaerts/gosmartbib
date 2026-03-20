package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.entities.SchoolEntity;

public record SchoolDTO(
        Long id,
        String name,
        String domain) {
    public static SchoolDTO from(SchoolEntity school) {
        return new SchoolDTO(
                school.getId(),
                school.getName(),
                school.getDomain());
    }
}
