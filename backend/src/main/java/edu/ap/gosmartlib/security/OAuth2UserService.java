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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In deze klasse zorgen we ervoor dat Spring Security meer dan 1 attribuut
 * meeneemt in memory na login
 * Spring Security haalt standaard de userID, platform en isMainAccount op van
 * Smartschool, maar we willen ook de groepen van de gebruiker in memory hebben.
 * Daarom maken we een custom OAuth2UserService die na het ophalen van de
 * basisinformatie van nog een extra API call roept voor de groupInfo
 * Na aanroep mergen we deze informatie bij de fulluserinfo
 */

@Slf4j
@Component
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userReq) throws OAuth2AuthenticationException {

        // Stap 1. Spring Security haalt de fulluserinfo
        // dit gebeurt via user-info-uri van in de application.properties
        // en returnt een OAuth2User met alle benodigde info
        OAuth2User fulluserinfo = super.loadUser(userReq);

        log.info("=== FULLUSERINFO ===");
        fulluserinfo.getAttributes().forEach((k, v) -> log.info("  {} = {}", k, v));
        log.info("====================");

        // Stap 2. we halen de accessToken en het platform zodat we groupinfo kunnen
        // ophalen
        String accessToken = userReq.getAccessToken().getTokenValue();
        String platform = fulluserinfo.getAttribute("platform");

        Map<String, Object> groupInfoResponse = fetchGroupInfo(platform, accessToken);

        log.info("=== GROUPINFO ===");

        List<Map<String, Object>> groups = (List<Map<String, Object>>) groupInfoResponse.get("groups");
        List<Map<String, Object>> parentGroups = (List<Map<String, Object>>) groupInfoResponse.get("parentGroups");

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
     * Roept smartschools /Api/V1/groupinfo endpoint aan via de accessToken
     * 
     * @param platform    - het platform van de gebruiker, nodig om de juiste URL
     *                    aan te roepen (lokaal of productie)
     * @param accessToken - de access token die we gekregen hebben van Spring
     *                    Security
     * @return raw responsemap met "groups" en "parentGroups"
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
}
