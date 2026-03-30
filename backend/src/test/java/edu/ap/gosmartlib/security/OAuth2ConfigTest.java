package edu.ap.gosmartlib.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2ConfigTest {

    @Test
    void authorizationRequestRepository_shouldReturnHttpSessionRepository() {
        OAuth2Config config = new OAuth2Config();

        AuthorizationRequestRepository<OAuth2AuthorizationRequest> repository = config.authorizationRequestRepository();

        assertNotNull(repository);
        assertTrue(repository.getClass().getSimpleName().contains("HttpSessionOAuth2AuthorizationRequestRepository"));
    }

    @Test
    void pkceDisabledResolver_shouldResolveRequestWithoutPkceParameters() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("smartschool")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("fulluserinfo")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/oauth/userinfo")
                .userNameAttributeName("userID")
                .build();

        OAuth2Config config = new OAuth2Config();
        OAuth2AuthorizationRequestResolver resolver = config.pkceDisabledResolver(
                new InMemoryClientRegistrationRepository(registration));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/smartschool");
        OAuth2AuthorizationRequest authRequest = resolver.resolve(request);

        assertNotNull(authRequest);
        assertFalse(authRequest.getAdditionalParameters().containsKey("code_challenge"));
        assertFalse(authRequest.getAdditionalParameters().containsKey("code_challenge_method"));
        assertFalse(authRequest.getAttributes().containsKey("code_verifier"));
    }
}

