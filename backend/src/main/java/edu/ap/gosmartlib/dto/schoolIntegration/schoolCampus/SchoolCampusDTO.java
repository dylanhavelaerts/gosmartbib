package edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolCampusEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolCampusEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca

public record SchoolCampusDTO(
        Long id,
        String name) {

    public static SchoolCampusDTO from(SchoolCampusEntity campus) {
        return new SchoolCampusDTO(
                campus.getId(),
                campus.getName());
    }
}