package edu.ap.testbackend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Deze klasse fungeert als een aangepaste implementatie van AuthorizationRequestRepository die gebruikmaakt van de 
 * HttpSessionOAuth2AuthorizationRequestRepository als delegate. 
 * Het doel van deze klasse is om de opslag en verwijdering van OAuth2AuthorizationRequest-objecten 
 * te beheren tijdens het OAuth2-authenticatieproces. 
 * Door deze implementatie kunnen we ervoor zorgen dat de autorisatieverzoeken correct worden 
 * opgeslagen in de HTTP-sessie en dat ze kunnen worden opgehaald of verwijderd wanneer dat nodig is, 
 * wat essentieel is voor een soepele OAuth2-loginervaring met Smartschool.
 */
@Component
public class AuthRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String AUTH_REQUEST_ATTR = AuthRepository.class.getName() + ".AUTHORIZATION_REQUEST";

    /**
     * Laadt het OAuth2AuthorizationRequest-object uit de HTTP-sessie op basis van het inkomende verzoek.
     * @param request Het inkomende HTTP-verzoek dat mogelijk een autorisatieverzoek bevat
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return null;
        }

        Object value = request.getSession(false).getAttribute(AUTH_REQUEST_ATTR);
        if (!(value instanceof OAuth2AuthorizationRequest savedRequest)) {
            return null;
        }

        String callbackState = request.getParameter("state");
        if (!StringUtils.hasText(callbackState)) {
            // Smartschool callback may omit state; fall back to the single saved request in session.
            return savedRequest;
        }

        return callbackState.equals(savedRequest.getState()) ? savedRequest : null;
    }

    /**
     * Slaat het gegeven OAuth2AuthorizationRequest-object op in de HTTP-sessie, 
     * zodat het later kan worden opgehaald tijdens het authenticatieproces.
     */
    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        request.getSession(true).setAttribute(AUTH_REQUEST_ATTR, authorizationRequest);
    }

    /**
    * Verwijdert het OAuth2AuthorizationRequest-object uit de HTTP-sessie en retourneert het verwijderde object.
    * Dit wordt meestal aangeroepen nadat het autorisatieproces is voltooid, om op te ruimen.
    */  
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest saved = loadAuthorizationRequest(request);
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(AUTH_REQUEST_ATTR);
        }
        return saved;
    }
}