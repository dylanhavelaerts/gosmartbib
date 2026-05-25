package edu.ap.gosmartlib.dto.school;

<<<<<<< HEAD
import edu.ap.gosmartlib.entities.schoolEntities.SchoolEntity;
=======
import edu.ap.gosmartlib.entities.school.SchoolEntity;
>>>>>>> 4f936deee088529c0230b4241700c7fa8a61f9ca

public record SchoolDTO(
        Long id,
        String name,
        String domain,
        boolean adminApproved
) {
    public static SchoolDTO from(SchoolEntity school) {
        return new SchoolDTO(school.getId(), school.getName(), school.getDomain(), school.isAdminApproved());
    }
}
