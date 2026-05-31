package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.UserEntity;


/**
 * Abstractie voor het versturen van berichten naar gebruikers.
 * De primaire implementatie verstuurt via de Smartschool SOAP-API.
 */
public interface MessageSender {
    void sendMessage(UserEntity user, String title, String body);

}
