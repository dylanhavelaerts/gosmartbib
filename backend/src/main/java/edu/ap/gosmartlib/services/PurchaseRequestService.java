package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.purchaseRequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaseRequest.PurchaseRequestDTO;
import edu.ap.gosmartlib.entities.PurchaseRequestEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.PurchaseRequestRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.util.PurchaseStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;

    public PurchaseRequestDTO createRequest(CreatePurchaseRequestDTO dto, String smartschoolUid) {
        if (dto.title() == null || dto.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titel is verplicht");
        }

        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid).orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden"));

        PurchaseRequestEntity entity = new PurchaseRequestEntity();
        entity.setTitle(dto.title().trim());
        entity.setAuthors(dto.authors() == null ? List.of() :
                dto.authors().stream()
                        .filter(a -> a != null && !a.isBlank())
                        .map(String::trim)
                        .toList());
        entity.setIsbn(dto.isbn() == null || dto.isbn().isBlank() ? null : dto.isbn().trim());
        entity.setUser(user);

        return toDTO(purchaseRequestRepository.save(entity));
    }

    public List<PurchaseRequestDTO> findAllForSchool(String smartschoolUid) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid).orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden"));

        return purchaseRequestRepository.findByUser_School_Id(user.getSchool().getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public PurchaseRequestDTO approveRequest(Long id, String note) {
        PurchaseRequestEntity entity = purchaseRequestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Aankoopverzoek niet gevonden"));

        entity.setStatus(PurchaseStatus.APPROVED);
        entity.setNote(note == null || note.isBlank() ? null : note.trim());

        return toDTO(purchaseRequestRepository.save(entity));
    }

    public PurchaseRequestDTO rejectRequest(Long id, String note) {
        PurchaseRequestEntity entity = purchaseRequestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Aankoopverzoek niet gevonden"));

        entity.setStatus(PurchaseStatus.REJECTED);
        entity.setNote(note == null || note.isBlank() ? null : note.trim());

        return toDTO(purchaseRequestRepository.save(entity));
    }

    public void deleteRequest(Long id) {
        if (!purchaseRequestRepository.existsById(id)) {
            throw new EntityNotFoundException("Aankoopverzoek niet gevonden");
        }
        purchaseRequestRepository.deleteById(id);
    }

    private PurchaseRequestDTO toDTO(PurchaseRequestEntity entity) {
        return new PurchaseRequestDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getAuthors(),
                entity.getIsbn(),
                entity.getStatus(),
                entity.getUser().getId(),
                entity.getUser().getSmartschoolUid(),
                entity.getRequestDate(),
                entity.getNote()
        );
    }
}
