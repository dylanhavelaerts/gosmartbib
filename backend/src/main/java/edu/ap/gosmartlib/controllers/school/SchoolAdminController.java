package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.dto.school.UpdateSchoolRequest;
import edu.ap.gosmartlib.services.school.SchoolAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/schools")
@RequiredArgsConstructor
public class SchoolAdminController {

    private final SchoolAdminService schoolAdminService;

    @GetMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<SchoolDTO> listSchools() {
        return schoolAdminService.listAllSchools();
    }

    @PostMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public ResponseEntity<SchoolDTO> createSchool(@RequestBody CreateSchoolRequest request) {
        var result = schoolAdminService.createSchool(request);
        HttpStatus status = result.alreadyExisted() ? HttpStatus.CONFLICT : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.school());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolDTO approveSchool(@PathVariable Long id, @RequestBody ApproveSchoolRequest request) {
        return schoolAdminService.approveSchool(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public void deleteSchool(@PathVariable Long id) {
        schoolAdminService.deleteSchool(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolDTO renameSchool(@PathVariable Long id, @RequestBody UpdateSchoolRequest request) {
        return schoolAdminService.renameSchool(id, request);
    }

}
