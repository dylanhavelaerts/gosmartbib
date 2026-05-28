package edu.ap.gosmartlib.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom OAuth2UserService voor Smartschoolgebruikers.
 *
 * Spring Security haalt standaard fulluserinfo op via de user-info-uri.
 * GoSmartLib
 * heeft daarnaast ook groupinfo nodig om klassen en parentgroepen te kunnen
 * synchroniseren. Deze service haalt daarom na fulluserinfo nog groupinfo op en
 * voegt beide bronnen samen in één OAuth2User.
 */
@Slf4j
@Component
public class OAuth2UserService extends DefaultOAuth2UserService {

    /**
     * Laadt de Smartschoolgebruiker en verrijkt die met groupinfo.
     *
     * Eerst haalt Spring Security fulluserinfo op. Daarna wordt met dezelfde access
     * token de Smartschool groupinfo endpoint aangeroepen. De groepen en
     * parentgroepen worden toegevoegd aan de OAuth2User-attributen, zodat
     * UserService
     * later schoolklassen kan synchroniseren.
     *
     * @param userReq OAuth2 user request met clientregistratie en access token
     * @return OAuth2User met fulluserinfo, groups en parentGroups
     * @throws OAuth2AuthenticationException wanneer de standaard userinfo-ophaling
     *                                       faalt
     */

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userReq) throws OAuth2AuthenticationException {

        // Stap 1. Spring Security haalt de fulluserinfo
        // dit gebeurt via user-info-uri van in de application.properties
        // en returnt een OAuth2User met alle benodigde info
        OAuth2User fulluserinfo = super.loadUser(userReq);

        // Stap 2. we halen de accessToken en het platform zodat we groupinfo kunnen
        // ophalen
        String accessToken = userReq.getAccessToken().getTokenValue();
        String platform = fulluserinfo.getAttribute("platform");

        Map<String, Object> groupInfoResponse = fetchGroupInfo(platform, accessToken);


        List<Map<String, Object>> groups = extractGroupList(groupInfoResponse.get("groups"));
        List<Map<String, Object>> parentGroups = extractGroupList(groupInfoResponse.get("parentGroups"));

        // goede formatting
        if (groups != null) {
            log.info("  groups:");
            groups.forEach(group -> group.forEach((k, v) -> log.info("    {} = {}", k, v)));
        }

        if (parentGroups != null) {
            log.info("  parentGroups:");
            parentGroups.forEach(group -> group.forEach((k, v) -> log.info("    {} = {}", k, v)));
        }

        log.info("=================");

        // Stap 3. voeg de twee bronnen samen in memory
        Map<String, Object> mergedAttributes = new HashMap<>(fulluserinfo.getAttributes());
        mergedAttributes.put("groups", groupInfoResponse.getOrDefault("groups", Collections.emptyList()));
        mergedAttributes.put("parentGroups", groupInfoResponse.getOrDefault("parentGroups", Collections.emptyList()));

        // Stap 4. return de OAuth2User met de gemenge attributen
        return new DefaultOAuth2User(
                fulluserinfo.getAuthorities(),
                mergedAttributes,
                "userID");
    }

    /**
     * Haalt groupinfo op bij Smartschool.
     *
     * Wanneer deze call faalt, wordt de login niet volledig geblokkeerd. In dat
     * geval
     * wordt een lege map teruggegeven en kan de gebruiker nog steeds worden
     * aangemeld, maar zonder geüpdatete groepsinformatie.
     *
     * @param platform    Smartschoolplatform van de gebruiker
     * @param accessToken access token verkregen via de OAuth2 token exchange
     * @return responsemap met groups en parentGroups, of een lege map bij fouten
     */
    private Map<String, Object> fetchGroupInfo(String platform, String accessToken) {
        try {
            String url = platform + "/Api/V1/groupinfo?access_token=" + accessToken;

            Map<String, Object> result = RestClient.create()
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            return result != null ? result : Collections.emptyMap();

        } catch (Exception e) {
            log.warn("Failed to fetch groupinfo from Smartschool: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Normaliseert een ruwe Smartschool groupinfo-lijst naar een lijst van maps.
     *
     * Smartschool geeft geneste JSON-structuren terug. Deze helper zet elke entry
     * om
     * naar een Map<String, Object>, zodat de rest van de applicatie veilig met de
     * groepsdata kan werken.
     *
     * @param value ruwe waarde uit de Smartschool groupinfo response
     * @return genormaliseerde lijst van groepsmaps, of null wanneer de waarde geen
     *         lijst is
     */

    private List<Map<String, Object>> extractGroupList(Object value) {
        if (!(value instanceof List<?> list)) {
            return null;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> entry = new HashMap<>();
                for (Map.Entry<?, ?> pair : map.entrySet()) {
                    if (pair.getKey() != null) {
                        entry.put(pair.getKey().toString(), pair.getValue());
                    }
                }
                result.add(entry);
            }
        }
        return result;
    }
}
