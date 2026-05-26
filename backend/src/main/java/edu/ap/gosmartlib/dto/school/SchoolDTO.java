package edu.ap.gosmartlib.dto.school;

import edu.ap.gosmartlib.entities.school.SchoolEntity;

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
