package edu.ap.gosmartlib.services.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.gosmartlib.dto.AdminUserDTO;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.UserRoles;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> listUsersForAdmin(String actorUid, Long schoolId, String name, Pageable pageable) {
        UserEntity actor = getCurrentAdmin(actorUid);
        Long effectiveSchoolId = resolveSchoolId(actor, schoolId);
        Page<UserEntity> users = userRepository.findBySchoolIdAndName(effectiveSchoolId, name, pageable);
        return users.map(AdminUserDTO::from);
    }

    @Transactional
    public AdminUserDTO updateUserRole(String actorUid, Long schoolId, Long targerUserId, UserRoles newRole) {
        if (newRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        }

        if (newRole == UserRoles.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Gebruikers mogen niet naar ADMIN worden aangepast");
        }

        UserEntity actor = getCurrentAdmin(actorUid);
        Long effectiveSchoolId = resolveSchoolId(actor, schoolId);

        UserEntity target = userRepository.findByIdAndSchool_Id(targerUserId, effectiveSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (target.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Je kan je eigen rol niet aanpassen");
        }

        target.setRole(newRole);

        return AdminUserDTO.from(userRepository.save(target));
    }
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> listUsersAsPlatformAdmin(Long schoolId, String name, Pageable pageable) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        return userRepository.findBySchoolIdAndName(schoolId, name, pageable).map(AdminUserDTO::from);
    }

    @Transactional
    public AdminUserDTO updateUserRoleAsPlatformAdmin(Long schoolId, Long targetUserId, UserRoles newRole) {
        if (newRole == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        if (newRole == UserRoles.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ongeldige rol");
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        target.setRole(newRole);
        return AdminUserDTO.from(userRepository.save(target));
    }


    private Long resolveSchoolId(UserEntity actor, Long schoolId) {
        if (actor.getRole() == UserRoles.ADMIN) {
            if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
            return schoolId;
        }
        return actor.getSchool().getId();
    }

    @Transactional(readOnly = true)
    protected UserEntity getCurrentAdmin(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));

        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER && actor.getRole() != UserRoles.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        }

        return actor;
    }
}