package edu.ap.gosmartlib.dto;

import java.util.Set;
import java.util.stream.Collectors;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.util.UserRoles;

public record AdminUserDTO(
        long id,
        String smartschoolUid,
        UserRoles role,
        boolean active,
        SchoolDTO school,
        Set<SchoolClassDTO> classes) {
    public static AdminUserDTO from(UserEntity user) {
        return new AdminUserDTO(user.getId(), user.getSmartschoolUid(), user.getRole(), user.isActive(),
                SchoolDTO.from(user.getSchool()),
                user.getClasses().stream().map(SchoolClassDTO::from).collect(Collectors.toSet()));
    }
}
