package edu.ap.gosmartlib.controllers.readinglist;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListAssignmentTargetsDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.UpdateReadingListVisibilityDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.ReadingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-controller voor persoonlijke leeslijsten, klasleeslijsten en publieke
 * deellinks.
 */
@RestController
@RequestMapping("/reading-lists")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;
    private final AuthHelper authHelper;

    /**
     * Haalt alle leeslijsten op die zichtbaar zijn voor de aangemelde gebruiker.
     *
     * @param principal de aangemelde OAuth2-gebruiker
     * @return overzicht van persoonlijke lijsten en zichtbare klaslijsten
     */
    @GetMapping
    public ResponseEntity<List<ReadingListOverviewDTO>> getVisibleLists(@AuthenticationPrincipal OAuth2User principal) {
        List<ReadingListOverviewDTO> lists = readingListService.getVisibleLists(authHelper.extractUid(principal));
        return ResponseEntity.ok(lists);
    }

    /**
     * Haalt de details van één leeslijst op.
     *
     * @param id        interne leeslijst-ID
     * @param principal de aangemelde OAuth2-gebruiker
     * @return detailweergave van de leeslijst
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReadingListDetailDTO> getListDetail(@PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListDetailDTO detail = readingListService.getListDetail(id, authHelper.extractUid(principal));
        return ResponseEntity.ok(detail);
    }

    /**
     * Haalt een publiek gedeelde persoonlijke leeslijst op via haar publieke UUID.
     *
     * @param publicUid publieke UUID van de leeslijst
     * @return publieke detailweergave zonder persoonlijke targetgegevens
     */
    @GetMapping("/shared/{publicUid}")
    public ResponseEntity<PublicReadingListDetailDTO> getPublicListDetail(@PathVariable String publicUid) {
        PublicReadingListDetailDTO detail = readingListService.getPublicListDetail(publicUid);
        return ResponseEntity.ok(detail);
    }

    /**
     * Maakt een persoonlijke leeslijst aan voor de aangemelde gebruiker.
     *
     * @param dto       gegevens van de nieuwe leeslijst
     * @param principal de aangemelde OAuth2-gebruiker
     * @return ID van de aangemaakte leeslijst
     */
    @PostMapping("/personal")
    public ResponseEntity<Long> createPersonalList(@RequestBody CreateReadingListDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity created = readingListService.createPersonalList(dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(created.getId());
    }

    /**
     * Past een persoonlijke leeslijst van de aangemelde gebruiker aan.
     *
     * @param id        interne leeslijst-ID
     * @param dto       nieuwe gegevens van de leeslijst
     * @param principal de aangemelde OAuth2-gebruiker
     * @return ID van de aangepaste leeslijst
     */
    @PutMapping("/personal/{id}")
    public ResponseEntity<Long> updatePersonalList(
            @PathVariable Long id,
            @RequestBody CreateReadingListDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity updated = readingListService.updatePersonalList(id, dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(updated.getId());
    }

    /**
     * Haalt vaste doelgroeplijsten op voor het aanmaken van klasleeslijsten.
     *
     * @param principal de aangemelde OAuth2-gebruiker
     * @return beschikbare klassen, jaren en graden
     */
    @GetMapping("/assignment-targets")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<ReadingListAssignmentTargetsDTO> getAssignmentTargets(
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListAssignmentTargetsDTO targets = readingListService
                .getAssignmentTargets(authHelper.extractUid(principal));
        return ResponseEntity.ok(targets);
    }

    /**
     * Zoekt leerlingen die als specifieke doelgroep voor een klasleeslijst gekozen
     * kunnen worden.
     *
     * @param query     zoekterm voor naam, gebruikersnaam of klas
     * @param principal de aangemelde OAuth2-gebruiker
     * @return maximaal twintig leerlingmatches binnen de eigen school
     */
    @GetMapping("/assignment-targets/students")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<?> searchAssignmentStudents(
            @RequestParam(defaultValue = "") String query,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(readingListService.searchAssignmentStudents(authHelper.extractUid(principal), query));
    }

    /**
     * Zoekt klassen die als doelgroep voor een klasleeslijst gekozen kunnen worden.
     *
     * @param query     optionele zoekterm voor de klasnaam
     * @param principal de aangemelde OAuth2-gebruiker
     * @return klasmatches binnen de eigen school
     */
    @GetMapping("/assignment-targets/classes")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<?> searchAssignmentClasses(
            @RequestParam(defaultValue = "") String query,
            @AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(readingListService.searchAssignmentClasses(authHelper.extractUid(principal), query));
    }

    /**
     * Verwijdert een persoonlijke leeslijst van de aangemelde gebruiker.
     *
     * @param id        interne leeslijst-ID
     * @param principal de aangemelde OAuth2-gebruiker
     * @return lege respons wanneer de lijst verwijderd is
     */
    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deletePersonalList(@PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deletePersonalList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    /**
     * Wijzigt of een persoonlijke leeslijst publiek deelbaar is.
     *
     * @param id        interne leeslijst-ID
     * @param dto       payload met de gewenste publieke zichtbaarheid
     * @param principal de aangemelde OAuth2-gebruiker
     * @return nieuwe deelstatus van de leeslijst
     */
    @PatchMapping("/personal/{id}/visibility")
    public ResponseEntity<ReadingListVisibilityDTO> updatePersonalListVisibility(
            @PathVariable Long id,
            @RequestBody UpdateReadingListVisibilityDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListVisibilityDTO updated = readingListService.updatePersonalListVisibility(
                id,
                dto.publicVisible(),
                authHelper.extractUid(principal));
        return ResponseEntity.ok(updated);
    }

    /**
     * Maakt een nieuwe klasleeslijst aan.
     *
     * @param dto       gegevens van de nieuwe klasleeslijst
     * @param principal de aangemelde OAuth2-gebruiker
     * @return ID van de aangemaakte klasleeslijst
     */
    @PostMapping("/class")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<Long> createClassList(@RequestBody CreateReadingListDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity created = readingListService.createClassList(dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(created.getId());
    }

    /**
     * Verwijdert een klasleeslijst waarvan de aangemelde gebruiker de maker is.
     *
     * @param id        interne leeslijst-ID
     * @param principal de aangemelde OAuth2-gebruiker
     * @return lege respons wanneer de lijst verwijderd is
     */
    @DeleteMapping("/class/{id}")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<Void> deleteClassList(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        readingListService.deleteClassList(id, authHelper.extractUid(principal));
        return ResponseEntity.noContent().build();
    }

    /**
     * Past een klasleeslijst aan waarvan de aangemelde gebruiker de maker is.
     *
     * @param id        interne leeslijst-ID
     * @param dto       nieuwe gegevens van de klasleeslijst
     * @param principal de aangemelde OAuth2-gebruiker
     * @return ID van de aangepaste klasleeslijst
     */
    @PutMapping("/class/{id}")
    @PreAuthorize("@roleGuard.isTeacherOrLibrarian(authentication)")
    public ResponseEntity<Long> updateClassList(@PathVariable Long id, @RequestBody CreateReadingListDTO dto,
            @AuthenticationPrincipal OAuth2User principal) {
        ReadingListEntity updated = readingListService.updateClassList(id, dto, authHelper.extractUid(principal));
        return ResponseEntity.ok(updated.getId());
    }
}
