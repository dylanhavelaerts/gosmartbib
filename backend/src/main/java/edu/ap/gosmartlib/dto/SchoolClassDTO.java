package edu.ap.gosmartlib.dto;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolClassEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolClassEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca

public record SchoolClassDTO(
        Long id,
        String name,
        String grade,
        String schoolYear) {
    public static SchoolClassDTO from(SchoolClassEntity schoolClass) {
        return new SchoolClassDTO(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getGrade(),
                schoolClass.getSchoolYear());
    }
}