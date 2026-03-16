package edu.ap.testbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Deze configuratieklasse definieert een aangepaste OAuth2AuthorizationRequestResolver die PKCE-parameters verwijdert
 * voor de Smartschool OAuth2-provider. Dit is nodig omdat Smartschool PKCE niet ondersteunt, terwijl Spring Security
 * standaard PKCE gebruikt voor alle authorization_code flows. Door deze resolver te gebruiken, kunnen we ervoor zorgen
 * dat de gegenereerde autorisatieverzoeken compatibel zijn met Smartschool's OAuth2-implementatie, waardoor gebruikers zich succesvol kunnen authenticeren via Smartschool zonder PKCE-gerelateerde fouten.
 */
@Configuration
public class OAuth2Config {

    @Bean
    public OAuth2AuthorizationRequestResolver pkceDisabledResolver(ClientRegistrationRepository repo) {
        // We starten met de standaard resolver die Spring Security biedt
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        // We passen de resolver aan om PKCE-parameters te verwijderen voor alle autorisatieverzoeken
        resolver.setAuthorizationRequestCustomizer(customizer -> {
            customizer.attributes(attrs -> {
                attrs.remove("code_verifier");
            });
            customizer.additionalParameters(params -> {
                params.remove("code_challenge");
                params.remove("code_challenge_method");
            });
        });

        return resolver;
    }
}