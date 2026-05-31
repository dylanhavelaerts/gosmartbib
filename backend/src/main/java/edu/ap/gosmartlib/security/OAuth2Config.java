package edu.ap.gosmartlib.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * OAuth2-configuratie voor Smartschool.
 *
 * Deze klasse past het standaard Spring Security authorization request aan.
 * Smartschool ondersteunt in deze koppeling geen PKCE-parameters, daarom worden
 * code_verifier, code_challenge en code_challenge_method verwijderd voordat de
 * browser naar Smartschool wordt doorgestuurd.
 */

@Configuration
public class OAuth2Config {

    /**
     * Maakt een authorization request resolver die PKCE-parameters verwijdert.
     *
     * Spring Security voegt standaard PKCE toe aan OAuth2 authorization requests.
     * De gebruikte Smartschool OAuth-koppeling ondersteunt deze PKCE-parameters
     * echter niet correct. Wanneer code_verifier, code_challenge of
     * code_challenge_method meegestuurd worden, kan de Smartschool authorization
     * flow falen.
     *
     * Daarom verwijdert deze resolver de PKCE-parameters vóór de redirect naar
     * Smartschool. Dit is een bewuste compatibiliteitskeuze, geen algemene
     * aanbeveling om PKCE uit te schakelen.
     *
     * Omdat PKCE normaal extra bescherming biedt tegen authorization-code
     * interception, moet de rest van de flow extra strikt blijven:
     * <ul>
     *   <li>HTTPS in productie;</li>
     *   <li>exacte redirect URI's;</li>
     *   <li>bescherming van de client secret;</li>
     *   <li>Spring Security state/session-validatie;</li>
     *   <li>token exchange alleen server-side in de backend.</li>
     * </ul>
     *
     * @param repo repository met de geconfigureerde OAuth2 clientregistraties
     * @return resolver die Smartschool-compatible authorization requests maakt
     */

    @Bean
    public OAuth2AuthorizationRequestResolver pkceDisabledResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(repo,
                "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            // Verwijder PKCE-parameters die Spring Security standaard toevoegt, omdat
            // Smartschool deze niet ondersteunt.
            customizer.attributes(attrs -> attrs.remove("code_verifier"));
            customizer.additionalParameters(params -> {
                params.remove("code_challenge");
                params.remove("code_challenge_method");
            });
        });

        return resolver;
    }

    /**
     * Slaat OAuth2 authorization requests tijdelijk op in de HTTP-sessie.
     *
     * Spring Security gebruikt deze repository om de state tussen de initiële
     * authorization redirect en de callback met authorization code te bewaren.
     *
     * @return sessiegebaseerde repository voor OAuth2 authorization requests
     */

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }
}