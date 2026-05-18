package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.school.ApproveSchoolRequest;
import edu.ap.gosmartlib.dto.school.CreateSchoolRequest;
import edu.ap.gosmartlib.dto.school.SchoolDTO;
import edu.ap.gosmartlib.services.school.SchoolAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public SchoolDTO createSchool(@RequestBody CreateSchoolRequest request) {
        return schoolAdminService.createSchool(request);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolDTO approveSchool(@PathVariable Long id, @RequestBody ApproveSchoolRequest request) {
        return schoolAdminService.approveSchool(id, request);
    }
}
