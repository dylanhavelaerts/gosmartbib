package edu.ap.gosmartlib.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class SecretCryptoService {

    @Value("${app.crypto.password}")
    private String password;

    @Value("${app.crypto.salt}")
    private String salt;

    private TextEncryptor encryptor;

    @PostConstruct
    public void init() {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("app.crypto.password is missing");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException("app.crypto.salt is missing");
        }

        this.encryptor = Encryptors.delux(password, salt);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null)
            return null;
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null)
            return null;
        return encryptor.decrypt(ciphertext);
    }
}