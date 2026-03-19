package edu.ap.testbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
public class OAuth2Config {

    @Bean
    public OAuth2AuthorizationRequestResolver pkceDisabledResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            // Verwijder PKCE-parameters die Spring Security standaard toevoegt, omdat Smartschool deze niet ondersteunt.
            customizer.attributes(attrs -> attrs.remove("code_verifier"));
            customizer.additionalParameters(params -> {
                params.remove("code_challenge");
                params.remove("code_challenge_method");
            });
        });

        return resolver;
    }

    // Spring Security gebruikt deze bean om de OAuth2-autorisatieverzoeken op te slaan tijdens het authenticatieproces.
    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }
}