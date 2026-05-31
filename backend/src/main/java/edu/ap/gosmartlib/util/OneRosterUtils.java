package edu.ap.gosmartlib.util;

import java.util.Map;

public class OneRosterUtils {

    private OneRosterUtils() {}

    /**
     * Extraheert de Smartschool UID uit een OneRoster-gebruiker
     * De Smartschool UID is opgeslagen in de metadata van de OneRoster-gebruiker onder de sleutel "smsc.legacyIdentifier"
     * Deze methode controleert of de metadata aanwezig is en of de Smartschool UID een niet-lege string is voordat deze wordt geretourneerd
     * @param onerosterUser de OneRoster-gebruiker waarvan de Smartschool UID moet worden geëxtraheerd
     * @return de Smartschool UID, of null als deze niet beschikbaar is
     */
    public static String extractSmartschoolUid(Map<String, Object> onerosterUser) {
        Object metadata = onerosterUser.get("metadata");
        if (metadata instanceof Map<?, ?> meta) {
            Object uid = meta.get("smsc.legacyIdentifier");
            if (uid instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
