package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.purchaserequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestNoteDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final AuthHelper authHelper;

    @PostMapping
    @PreAuthorize("@roleGuard.isTeacherOrBibbeheerder(authentication)")
    public ResponseEntity<PurchaseRequestDTO> createRequest(@RequestBody CreatePurchaseRequestDTO dto, @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseRequestService.createRequest(dto, authHelper.extractUid(principal)));
    }

    @GetMapping
    @PreAuthorize("@roleGuard.isTeacherOrBibbeheerder(authentication)")
    public ResponseEntity<List<PurchaseRequestDTO>> getAllForSchool(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(purchaseRequestService.findAllForSchool(authHelper.extractUid(principal)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<PurchaseRequestDTO> approveRequest(@PathVariable Long id, @RequestBody PurchaseRequestNoteDTO note) {
        return ResponseEntity.ok(purchaseRequestService.approveRequest(id, note.note()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<PurchaseRequestDTO> rejectRequest(@PathVariable Long id, @RequestBody PurchaseRequestNoteDTO note) {
        return ResponseEntity.ok(purchaseRequestService.rejectRequest(id, note.note()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@roleGuard.isBibbeheerder(authentication)")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        purchaseRequestService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
