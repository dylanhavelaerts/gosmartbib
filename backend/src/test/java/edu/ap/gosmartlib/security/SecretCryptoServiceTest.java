package edu.ap.gosmartlib.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SecretCryptoServiceTest {

    private SecretCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new SecretCryptoService();
        ReflectionTestUtils.setField(cryptoService, "password", "test-password-minimum-32-characters");
        ReflectionTestUtils.setField(cryptoService, "salt", "deadbeefcafe1234");
        cryptoService.init();
    }

    @Test
    void givenPlaintext_whenEncryptThenDecrypt_thenReturnsOriginal() {
        String plaintext = "my-secret-token";
        assertEquals(plaintext, cryptoService.decrypt(cryptoService.encrypt(plaintext)));
    }

    @Test
    void givenNull_whenEncrypt_thenReturnsNull() {
        assertNull(cryptoService.encrypt(null));
    }

    @Test
    void givenNull_whenDecrypt_thenReturnsNull() {
        assertNull(cryptoService.decrypt(null));
    }

    @Test
    void givenSamePlaintext_whenEncryptedTwice_thenProducesDifferentCiphertexts() {
        assertNotEquals(cryptoService.encrypt("same"), cryptoService.encrypt("same"));
    }
}
