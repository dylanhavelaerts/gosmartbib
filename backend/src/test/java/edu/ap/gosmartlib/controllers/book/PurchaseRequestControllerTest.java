package edu.ap.gosmartlib.controllers.book;

import edu.ap.gosmartlib.dto.purchaserequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestNoteDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.PurchaseRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestControllerTest {

    @Mock private PurchaseRequestService purchaseRequestService;
    @Mock private AuthHelper authHelper;
    @Mock private OAuth2User principal;

    @InjectMocks
    private PurchaseRequestController purchaseRequestController;

    private static final String UID = "teacher-uid";

    @Test
    void givenValidPrincipal_whenCreateRequest_thenReturnsCreated() {
        CreatePurchaseRequestDTO dto = mock(CreatePurchaseRequestDTO.class);
        PurchaseRequestDTO created = mock(PurchaseRequestDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(purchaseRequestService.createRequest(dto, UID)).thenReturn(created);

        ResponseEntity<PurchaseRequestDTO> response = purchaseRequestController.createRequest(dto, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(created, response.getBody());
        verify(purchaseRequestService).createRequest(dto, UID);
    }

    @Test
    void givenValidPrincipal_whenGetAllForSchool_thenReturnsOkWithList() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(purchaseRequestService.findAllForSchool(UID)).thenReturn(List.of());

        ResponseEntity<List<PurchaseRequestDTO>> response = purchaseRequestController.getAllForSchool(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(purchaseRequestService).findAllForSchool(UID);
    }

    @Test
    void givenValidRequest_whenApproveRequest_thenReturnsOkWithUpdated() {
        PurchaseRequestNoteDTO note = new PurchaseRequestNoteDTO("Approved");
        PurchaseRequestDTO approved = mock(PurchaseRequestDTO.class);
        when(purchaseRequestService.approveRequest(1L, "Approved")).thenReturn(approved);

        ResponseEntity<PurchaseRequestDTO> response = purchaseRequestController.approveRequest(1L, note);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(approved, response.getBody());
        verify(purchaseRequestService).approveRequest(1L, "Approved");
    }

    @Test
    void givenValidRequest_whenRejectRequest_thenReturnsOkWithUpdated() {
        PurchaseRequestNoteDTO note = new PurchaseRequestNoteDTO("Out of budget");
        PurchaseRequestDTO rejected = mock(PurchaseRequestDTO.class);
        when(purchaseRequestService.rejectRequest(2L, "Out of budget")).thenReturn(rejected);

        ResponseEntity<PurchaseRequestDTO> response = purchaseRequestController.rejectRequest(2L, note);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(rejected, response.getBody());
        verify(purchaseRequestService).rejectRequest(2L, "Out of budget");
    }

    @Test
    void givenValidId_whenDeleteRequest_thenReturnsNoContent() {
        ResponseEntity<Void> response = purchaseRequestController.deleteRequest(3L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(purchaseRequestService).deleteRequest(3L);
    }
}
