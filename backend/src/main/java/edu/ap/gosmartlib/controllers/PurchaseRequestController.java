package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.purchaseRequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaseRequest.PurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaseRequest.PurchaseRequestNoteDTO;
import edu.ap.gosmartlib.exceptions.InvalidUserException;
import edu.ap.gosmartlib.services.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<PurchaseRequestDTO> createRequest(@RequestBody CreatePurchaseRequestDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        String uid = extractUid(principal);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseRequestService.createRequest(dto, uid));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<List<PurchaseRequestDTO>> getAllForSchool(@AuthenticationPrincipal OAuth2User principal) {
        String uid = extractUid(principal);

        return ResponseEntity.ok(purchaseRequestService.findAllForSchool(uid));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<PurchaseRequestDTO> approveRequest(@PathVariable Long id, @RequestBody PurchaseRequestNoteDTO note) {
        return ResponseEntity.ok(purchaseRequestService.approveRequest(id, note.note()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('BIBLIOTHEEKBEHEERDER')")
    public ResponseEntity<PurchaseRequestDTO> rejectRequest(@PathVariable Long id, @RequestBody PurchaseRequestNoteDTO note) {
        return ResponseEntity.ok(purchaseRequestService.rejectRequest(id, note.note()));
    }

    private String extractUid(OAuth2User principal) {
        if (principal == null)
            throw new InvalidUserException(HttpStatus.UNAUTHORIZED, "Niet ingelogd");

        String uid = principal.getAttribute("userID");
        if (uid == null || uid.isBlank())
            throw new InvalidUserException(HttpStatus.UNAUTHORIZED, "Geen geldige gebruiker");

        return uid;
    }
}
