package edu.ap.testbackend.dto;

import edu.ap.testbackend.entities.SchoolClassEntity;

public record SchoolClassDTO(
        Long id,
        String name,
        String grade,
        String schoolYear
) {
    public static SchoolClassDTO from(SchoolClassEntity schoolClass) {
        return new SchoolClassDTO(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getGrade(),
                schoolClass.getSchoolYear()
        );
    }
}