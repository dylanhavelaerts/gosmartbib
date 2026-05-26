package edu.ap.gosmartlib.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretCryptoConverterTest {

    @Mock private SecretCryptoService cryptoService;
    @InjectMocks private SecretCryptoConverter converter;

    // ─── convertToDatabaseColumn ──────────────────────────────────────────────

    @Test
    void givenNull_whenConvertToDatabaseColumn_thenReturnsNullWithoutCallingService() {
        assertNull(converter.convertToDatabaseColumn(null));
        verifyNoInteractions(cryptoService);
    }

    @Test
    void givenBlank_whenConvertToDatabaseColumn_thenReturnsBlankWithoutCallingService() {
        assertEquals("  ", converter.convertToDatabaseColumn("  "));
        verifyNoInteractions(cryptoService);
    }

    @Test
    void givenValue_whenConvertToDatabaseColumn_thenDelegatesToEncrypt() {
        when(cryptoService.encrypt("secret")).thenReturn("encrypted");
        assertEquals("encrypted", converter.convertToDatabaseColumn("secret"));
    }

    // ─── convertToEntityAttribute ─────────────────────────────────────────────

    @Test
    void givenNull_whenConvertToEntityAttribute_thenReturnsNullWithoutCallingService() {
        assertNull(converter.convertToEntityAttribute(null));
        verifyNoInteractions(cryptoService);
    }

    @Test
    void givenBlank_whenConvertToEntityAttribute_thenReturnsBlankWithoutCallingService() {
        assertEquals(" ", converter.convertToEntityAttribute(" "));
        verifyNoInteractions(cryptoService);
    }

    @Test
    void givenValue_whenConvertToEntityAttribute_thenDelegatesToDecrypt() {
        when(cryptoService.decrypt("encrypted")).thenReturn("secret");
        assertEquals("secret", converter.convertToEntityAttribute("encrypted"));
    }
}
