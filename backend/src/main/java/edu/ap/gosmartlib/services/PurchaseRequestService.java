package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.purchaserequest.CreatePurchaseRequestDTO;
import edu.ap.gosmartlib.dto.purchaserequest.PurchaseRequestDTO;
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

/**
 * Service voor het beheren van aankoopverzoeken. Biedt functionaliteit voor het aanmaken, goedkeuren, afwijzen en verwijderen van aankoopverzoeken.
 * Ook kunnen alle aankoopverzoeken voor een specifieke school worden opgehaald.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;

    /**
     * Maakt een nieuw aankoopverzoek aan voor de gebruiker met het opgegeven Smartschool UID
     * De titel van het verzoek is verplicht, auteurs en ISBN zijn optioneel
     * @param dto - De gegevens voor het aanmaken van het aankoopverzoek
     * @param smartschoolUid - De Smartschool UID van de gebruiker
     * @return - Het aangemaakte aankoopverzoek
     */
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

    /**
     * Haalt alle aankoopverzoeken op voor de school van de gebruiker met het opgegeven Smartschool UID
     * @param smartschoolUid - De Smartschool UID van de gebruiker wiens school de aankoopverzoeken moet worden opgehaald
     * @return - Een lijst van aankoopverzoeken voor de school van de gebruiker
     */
    public List<PurchaseRequestDTO> findAllForSchool(String smartschoolUid) {
        UserEntity user = userRepository.findBySmartschoolUid(smartschoolUid).orElseThrow(() -> new EntityNotFoundException("Gebruiker niet gevonden"));

        return purchaseRequestRepository.findByUser_School_Id(user.getSchool().getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Keurt een aankoopverzoek goed
     * @param id - Het ID van het aankoopverzoek
     * @param note - De opmerking bij de goedkeuring
     * @return - Het aangemaakte aankoopverzoek
     */
    public PurchaseRequestDTO approveRequest(Long id, String note) {
        PurchaseRequestEntity entity = purchaseRequestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Aankoopverzoek niet gevonden"));

        entity.setStatus(PurchaseStatus.APPROVED);
        entity.setNote(note == null || note.isBlank() ? null : note.trim());

        return toDTO(purchaseRequestRepository.save(entity));
    }

    /**
     * Keurt een aankoopverzoek af
     * @param id - Het ID van het aankoopverzoek
     * @param note - De opmerking bij de afkeuring
     * @return - Het aangemaakte aankoopverzoek
     */
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
