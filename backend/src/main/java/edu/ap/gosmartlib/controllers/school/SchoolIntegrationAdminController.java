package edu.ap.gosmartlib.controllers.school;

import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationDTO;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveClassesResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveSchoolsResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationLiveUsersResponse;
import edu.ap.gosmartlib.dto.schoolintegration.SchoolIntegrationTestResponse;
import edu.ap.gosmartlib.dto.schoolintegration.UpsertSchoolIntegrationRequest;
import edu.ap.gosmartlib.security.AdminPrincipal;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.schoolintegration.SchoolIntegrationAdminService;
import edu.ap.gosmartlib.services.schoolintegration.SchoolIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller voor het beheren en controleren van schoolintegraties.
 *
 * <p>
 * Deze controller wordt gebruikt om OneRoster-configuratie op te slaan,
 * de verbinding met Smartschool te testen en live OneRoster-data op te halen.
 * </p>
 */
@RestController
@RequestMapping("/admin/schools/{schoolId}/integration")
@RequiredArgsConstructor
public class SchoolIntegrationAdminController {

    private final SchoolIntegrationService schoolIntegrationService;
    private final SchoolIntegrationAdminService schoolIntegrationAdminService;
    private final AuthHelper authHelper;

    /**
     * Haalt de opgeslagen OneRoster-integratie van een school op.
     *
     * <p>
     * Platformbeheerders mogen elke schoolintegratie bekijken. Bij
     * bibliotheekbeheerders wordt de ingelogde gebruiker via de Smartschool UID
     * opgezocht en gecontroleerd op rol.
     * </p>
     *
     * @param schoolId       de school waarvan de integratie opgehaald wordt
     * @param authentication de ingelogde gebruiker
     * @return de opgeslagen OneRoster-configuratie en status
     */
    @GetMapping
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public SchoolIntegrationDTO getIntegration(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationService.getIntegrationForPlatformAdmin(schoolId);
        return schoolIntegrationService.getIntegration(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    /**
     * Maakt of wijzigt de OneRoster-integratie van een school.
     *
     * <p>
     * Deze endpoint is beperkt tot platformbeheerders, omdat hier gevoelige
     * configuratie zoals client ID, client secret en Smartschoolgegevens opgeslagen
     * worden.
     * </p>
     *
     * @param schoolId       de school waarvoor de integratie opgeslagen wordt
     * @param request        de nieuwe OneRoster-configuratie
     * @param authentication de ingelogde gebruiker
     * @return de opgeslagen integratie zonder gevoelige secrets
     */
    @PutMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SchoolIntegrationDTO upsertIntegration(@PathVariable Long schoolId,
            @RequestBody UpsertSchoolIntegrationRequest request,
            Authentication authentication) {
        return schoolIntegrationService.upsertIntegrationForPlatformAdmin(schoolId, request);
    }

    /**
     * Test de OneRoster-integratie van een school.
     *
     * <p>
     * Hierbij wordt gecontroleerd of een access token opgehaald kan worden
     * en of de OneRoster API bereikbaar is.
     * </p>
     *
     * @param schoolId       de school waarvoor de integratie getest wordt
     * @param authentication de ingelogde gebruiker
     * @return resultaat van de integratietest
     */
    @PostMapping("/test")
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public SchoolIntegrationTestResponse testIntegration(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.testIntegrationForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.testIntegration(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    /**
     * Haalt live scholen/organisaties op uit OneRoster.
     *
     * <p>
     * Deze data wordt gebruikt om te controleren of de ingestelde Smartschool-
     * omgeving bereikbaar is en de verwachte schoolinformatie teruggeeft.
     * </p>
     *
     * @param schoolId       de school waarvoor OneRoster-scholen opgehaald worden
     * @param authentication de ingelogde gebruiker
     * @return live scholen/organisaties uit OneRoster
     */
    @GetMapping("/live/schools")
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public SchoolIntegrationLiveSchoolsResponse getLiveSchools(@PathVariable Long schoolId,
            Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveSchoolsForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveSchools(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    /**
     * Haalt live gebruikers op uit OneRoster.
     *
     * <p>
     * Deze data wordt enkel opgehaald om de integratie te controleren en wordt
     * door dit endpoint niet opgeslagen in de lokale databank.
     * </p>
     *
     * @param schoolId       de school waarvoor gebruikers opgehaald worden
     * @param authentication de ingelogde gebruiker
     * @return live gebruikers uit OneRoster
     */
    @GetMapping("/live/users")
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public SchoolIntegrationLiveUsersResponse getLiveUsers(@PathVariable Long schoolId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveUsersForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveUsers(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

    /**
     * Haalt live klassen op uit OneRoster.
     *
     * <p>
     * Deze data wordt gebruikt om te controleren of de ingestelde Smartschool-
     * omgeving de verwachte klassen teruggeeft.
     * </p>
     *
     * @param schoolId       de school waarvoor klassen opgehaald worden
     * @param authentication de ingelogde gebruiker
     * @return live klassen uit OneRoster
     */
    @GetMapping("/live/classes")
    @PreAuthorize("@roleGuard.isLibrarianOrAdmin(authentication)")
    public SchoolIntegrationLiveClassesResponse getLiveClasses(@PathVariable Long schoolId,
            Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminPrincipal)
            return schoolIntegrationAdminService.getLiveClassesForPlatformAdmin(schoolId);
        return schoolIntegrationAdminService.getLiveClasses(
                authHelper.extractUid((OAuth2User) authentication.getPrincipal()), schoolId);
    }

}
