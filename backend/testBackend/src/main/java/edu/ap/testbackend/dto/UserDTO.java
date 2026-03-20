package edu.ap.testbackend.dto;

import edu.ap.testbackend.entities.UserEntity;
import edu.ap.testbackend.util.UserRoles;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDTO(
        Long id,
        UserRoles role,
        SchoolDTO school,
        String smartschoolUid,
        Set<SchoolClassDTO> classes
) {
    public static UserDTO from(UserEntity user) {
        return new UserDTO(
                user.getId(),
                user.getRole(),
                SchoolDTO.from(user.getSchool()),
                user.getSmartschoolUid(),
                user.getClasses().stream()
                        .map(SchoolClassDTO::from)
                        .collect(Collectors.toSet())
        );
    }
}