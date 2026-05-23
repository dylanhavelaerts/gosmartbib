package edu.ap.gosmartlib.dto;

import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.util.UserRoles;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDTO(
        Long id,
        String smartschoolUid,
        UserRoles role,
        SchoolDTO school,
        Set<SchoolClassDTO> classes) {
    public static UserDTO from(UserEntity user) {
        return new UserDTO(
                user.getId(),
                user.getSmartschoolUid(),
                user.getRole(),
                user.getSchool() != null ? SchoolDTO.from(user.getSchool()) : null,
                user.getClasses().stream()
                        .map(SchoolClassDTO::from)
                        .collect(Collectors.toSet()));
    }
}