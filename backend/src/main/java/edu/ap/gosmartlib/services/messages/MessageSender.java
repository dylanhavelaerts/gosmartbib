package edu.ap.gosmartlib.services.messages;

import edu.ap.gosmartlib.entities.UserEntity;

public interface MessageSender {
    void sendMessage(UserEntity user, String title, String body);

}
