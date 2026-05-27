package edu.ap.gosmartlib.services.users;

import edu.ap.gosmartlib.repositories.loan.LoanRepository;
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
    private final UserDeletionService userDeletionService;
    private final LoanRepository loanRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> listUsersForBibbeheerder(String actorUid, Long schoolId, String name, Pageable pageable) {
        UserEntity actor = getCurrentBibbeheerder(actorUid);
        Long effectiveSchoolId = resolveSchoolId(actor, schoolId);
        Page<UserEntity> users = userRepository.findBySchoolIdAndName(effectiveSchoolId, name, pageable);
        return users.map(AdminUserDTO::from);
    }

    @Transactional
    public AdminUserDTO updateUserRoleForBibbeheerder(String actorUid, Long schoolId, Long targerUserId, UserRoles newRole) {
        if (newRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        }

        if (newRole == UserRoles.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Gebruikers mogen niet naar ADMIN worden aangepast");
        }

        UserEntity actor = getCurrentBibbeheerder(actorUid);
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
    public Page<AdminUserDTO> listUsersForPlatformAdmin(Long schoolId, String name, Pageable pageable) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");
        return userRepository.findBySchoolIdAndName(schoolId, name, pageable).map(AdminUserDTO::from);
    }

    @Transactional
    public AdminUserDTO updateUserRoleForPlatformAdmin(Long schoolId, Long targetUserId, UserRoles newRole) {
        if (newRole == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieuwe rol ontbreekt");
        if (newRole == UserRoles.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ongeldige rol");
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));
        target.setRole(newRole);
        return AdminUserDTO.from(userRepository.save(target));
    }

    @Transactional
    public void deleteUserForBibbeheerder(String actorUid, Long targetUserId) {
        UserEntity actor = getCurrentBibbeheerder(actorUid);

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, actor.getSchool().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (target.getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Je kan je eigen account niet verwijderen");

        if (loanRepository.existsBySmartschoolUserId(target.getSmartschoolUid()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gebruiker heeft nog actieve leningen");

        userDeletionService.deleteUser(target);
    }

    @Transactional
    public void deleteUserForPlatformAdmin(Long schoolId, Long targetUserId) {
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId is verplicht");

        UserEntity target = userRepository.findByIdAndSchool_Id(targetUserId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gebruiker niet gevonden"));

        if (loanRepository.existsBySmartschoolUserId(target.getSmartschoolUid()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gebruiker heeft nog actieve leningen");

        userDeletionService.deleteUser(target);
    }


    private Long resolveSchoolId(UserEntity actor, Long schoolId) {
        return actor.getSchool().getId();
    }

    protected UserEntity getCurrentBibbeheerder(String actorUid) {
        UserEntity actor = userRepository.findDetailedBySmartschoolUid(actorUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingelogde gebruiker niet gevonden"));
        if (actor.getRole() != UserRoles.BIBLIOTHEEKBEHEERDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen toegang");
        return actor;
    }
}