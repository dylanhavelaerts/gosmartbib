package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.purchaserequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestDTO;
import edu.ap.gosmartlib.entities.PurchaseRequestEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.entities.school.SchoolEntity;
import edu.ap.gosmartlib.repositories.PurchaseRequestRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.PurchaseStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private PurchaseRequestService purchaseRequestService;

    private static final String UID = "student-uid";
    private static final Long SCHOOL_ID = 1L;

    private UserEntity user;
    private SchoolEntity school;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setId(SCHOOL_ID);

        user = new UserEntity();
        user.setId(10L);
        user.setSmartschoolUid(UID);
        user.setSchool(school);
    }

    // --- createRequest ---

    @Test
    void givenValidRequest_whenCreateRequest_thenSavesWithTrimmedFields() {
        CreatePurchaseRequestDTO dto = new CreatePurchaseRequestDTO("  Dune  ", List.of("Frank Herbert", "  "), "978-0441172719");
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> {
            PurchaseRequestEntity e = inv.getArgument(0);
            e = new PurchaseRequestEntity(e.getTitle(), e.getAuthors(), e.getIsbn(), e.getStatus(), e.getUser(), LocalDate.now(), null);
            return e;
        });

        PurchaseRequestDTO result = purchaseRequestService.createRequest(dto, UID);

        assertEquals("Dune", result.title());
        assertEquals(List.of("Frank Herbert"), result.authors());
        assertEquals("978-0441172719", result.isbn());
        assertEquals(PurchaseStatus.PENDING, result.status());
    }

    @Test
    void givenNullAuthorsAndBlankIsbn_whenCreateRequest_thenSavesWithEmptyAuthorsAndNullIsbn() {
        CreatePurchaseRequestDTO dto = new CreatePurchaseRequestDTO("Boek", null, "  ");
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> {
            PurchaseRequestEntity e = inv.getArgument(0);
            return new PurchaseRequestEntity(e.getTitle(), e.getAuthors(), e.getIsbn(), e.getStatus(), e.getUser(), LocalDate.now(), null);
        });

        PurchaseRequestDTO result = purchaseRequestService.createRequest(dto, UID);

        assertTrue(result.authors().isEmpty());
        assertNull(result.isbn());
    }

    @Test
    void givenBlankTitle_whenCreateRequest_thenThrows400() {
        CreatePurchaseRequestDTO dto = new CreatePurchaseRequestDTO("  ", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> purchaseRequestService.createRequest(dto, UID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(purchaseRequestRepository, never()).save(any());
    }

    @Test
    void givenUserNotFound_whenCreateRequest_thenThrowsEntityNotFoundException() {
        CreatePurchaseRequestDTO dto = new CreatePurchaseRequestDTO("Boek", null, null);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> purchaseRequestService.createRequest(dto, UID));

        verify(purchaseRequestRepository, never()).save(any());
    }

    // --- findAllForSchool ---

    @Test
    void givenUserWithSchool_whenFindAllForSchool_thenReturnsMappedDTOs() {
        PurchaseRequestEntity entity = buildRequest(1L, "Dune", PurchaseStatus.PENDING, null);
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.of(user));
        when(purchaseRequestRepository.findByUser_School_Id(SCHOOL_ID)).thenReturn(List.of(entity));

        List<PurchaseRequestDTO> result = purchaseRequestService.findAllForSchool(UID);

        assertEquals(1, result.size());
        assertEquals("Dune", result.get(0).title());
        assertEquals(PurchaseStatus.PENDING, result.get(0).status());
    }

    @Test
    void givenUserNotFound_whenFindAllForSchool_thenThrowsEntityNotFoundException() {
        when(userRepository.findBySmartschoolUid(UID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> purchaseRequestService.findAllForSchool(UID));
    }

    // --- approveRequest ---

    @Test
    void givenExistingRequest_whenApproveRequest_thenSetsStatusApprovedAndTrimsNote() {
        PurchaseRequestEntity entity = buildRequest(1L, "Dune", PurchaseStatus.PENDING, null);
        when(purchaseRequestRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestDTO result = purchaseRequestService.approveRequest(1L, "  Goedgekeurd  ");

        assertEquals(PurchaseStatus.APPROVED, result.status());
        assertEquals("Goedgekeurd", result.note());
    }

    @Test
    void givenExistingRequest_whenApproveRequestWithBlankNote_thenSetsNoteToNull() {
        PurchaseRequestEntity entity = buildRequest(1L, "Dune", PurchaseStatus.PENDING, null);
        when(purchaseRequestRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestDTO result = purchaseRequestService.approveRequest(1L, "  ");

        assertEquals(PurchaseStatus.APPROVED, result.status());
        assertNull(result.note());
    }

    @Test
    void givenRequestNotFound_whenApproveRequest_thenThrowsEntityNotFoundException() {
        when(purchaseRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> purchaseRequestService.approveRequest(99L, "note"));
    }

    // --- rejectRequest ---

    @Test
    void givenExistingRequest_whenRejectRequest_thenSetsStatusRejectedWithNote() {
        PurchaseRequestEntity entity = buildRequest(2L, "Foundation", PurchaseStatus.PENDING, null);
        when(purchaseRequestRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestDTO result = purchaseRequestService.rejectRequest(2L, "Budget overschreden");

        assertEquals(PurchaseStatus.REJECTED, result.status());
        assertEquals("Budget overschreden", result.note());
    }

    @Test
    void givenRequestNotFound_whenRejectRequest_thenThrowsEntityNotFoundException() {
        when(purchaseRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> purchaseRequestService.rejectRequest(99L, "note"));
    }

    // --- deleteRequest ---

    @Test
    void givenExistingRequest_whenDeleteRequest_thenDeletesById() {
        when(purchaseRequestRepository.existsById(1L)).thenReturn(true);

        purchaseRequestService.deleteRequest(1L);

        verify(purchaseRequestRepository).deleteById(1L);
    }

    @Test
    void givenRequestNotFound_whenDeleteRequest_thenThrowsEntityNotFoundException() {
        when(purchaseRequestRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> purchaseRequestService.deleteRequest(99L));

        verify(purchaseRequestRepository, never()).deleteById(any());
    }

    private PurchaseRequestEntity buildRequest(Long id, String title, PurchaseStatus status, String note) {
        PurchaseRequestEntity entity = new PurchaseRequestEntity(
                title, List.of(), null, status, user, LocalDate.now(), note);
        entity.setId(id);
        return entity;
    }
}
