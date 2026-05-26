package edu.ap.gosmartlib.dto.schoolintegration.schoolCampus;

import edu.ap.gosmartlib.entities.school.SchoolCampusEntity;

public record SchoolCampusDTO(
        Long id,
        String name) {

    public static SchoolCampusDTO from(SchoolCampusEntity campus) {
        return new SchoolCampusDTO(
                campus.getId(),
                campus.getName());
    }
}