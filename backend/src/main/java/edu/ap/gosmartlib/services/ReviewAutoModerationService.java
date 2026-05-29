package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.util.AutomaticModerationDecision;
import edu.ap.gosmartlib.util.ReviewFlagReason;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scant reviewteksten automatisch op ongepaste inhoud bij indienen of bewerken.
 * Configureeerbaar via:
 * review.moderation.blocked-words-resource: pad naar het woordenlijstbestand
 * review.moderation.max-links-in-review: maximaal aantal toegestane links (standaard 3)
 */
@Service
public class ReviewAutoModerationService {

    /** Herkent http://, https:// en www. links gevolgd door niet-witruimte tekens. */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE);

    /** Detecteert herhaling van hetzelfde karakter 10 of meer keer na elkaar (bv. "aaaaaaaaaa"). */
    private static final Pattern REPEATED_CHARACTER_PATTERN = Pattern.compile("(.)\\1{9,}");

    /** Verwijdert alle niet-alfanumerieke tekens bij normalisatie. */
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);

    /** Reduceert meerdere opeenvolgende spaties tot één spatie bij normalisatie. */
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Map<Character, String> LETTER_CLASS_MAP = buildLetterClassMap();

    private final ResourceLoader resourceLoader;
    private final String blockedWordsResource;
    private final int maxLinksInReview;

    private Set<String> blockedTerms = Set.of();
    private List<Pattern> blockedTermObfuscationPatterns = List.of();

    public ReviewAutoModerationService(ResourceLoader resourceLoader,
                                       @Value("${review.moderation.blocked-words-resource:classpath:moderation/blocked-review-words.txt}") String blockedWordsResource,
                                       @Value("${review.moderation.max-links-in-review:3}") int maxLinksInReview) {
        this.resourceLoader = resourceLoader;
        this.blockedWordsResource = blockedWordsResource;
        this.maxLinksInReview = maxLinksInReview;
    }

    /**
     * Laadt geblokkeerde woorden uit het geconfigureerde bestand bij opstart.
     * Bouwt ook obfuscatiepatronen per woord om leetspeak-varianten te detecteren.
     * Regels die beginnen met # worden beschouwd als commentaar en overgeslagen.
     */
    @PostConstruct
    public void loadBlockedTerms() {
        Set<String> loadedTerms = new LinkedHashSet<>();
        List<Pattern> loadedObfuscationPatterns = new ArrayList<>();

        try {
            Resource resource = resourceLoader.getResource(blockedWordsResource);
            if (!resource.exists()) {
                blockedTerms = Set.of();
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    String normalized = normalize(trimmed);
                    if (!normalized.isBlank()) {
                        loadedTerms.add(normalized);

                        Pattern obfuscationPattern = buildObfuscationPattern(normalized);
                        if (obfuscationPattern != null) {
                            loadedObfuscationPatterns.add(obfuscationPattern);
                        }
                    }
                }
            }

            blockedTerms = Set.copyOf(loadedTerms);
            blockedTermObfuscationPatterns = List.copyOf(loadedObfuscationPatterns);
        } catch (Exception ignored) {
            // Geen startup failure als het bestand niet leesbaar is.
            blockedTerms = Set.of();
            blockedTermObfuscationPatterns = List.of();
        }
    }

    /**
     * Scant een reviewtekst op ongepaste inhoud.
     * Controleert achtereenvolgens op geblokkeerde woorden, leetspeak-obfuscatie,
     * linkspam en karakterherhaling.
     *
     * @param text de te scannen reviewtekst, mag null zijn
     * @return AutomaticModerationDecision met flagged=true en een reden indien ongepast,
     *         anders flagged=false
     */
    public AutomaticModerationDecision moderate(String text) {
        String safeText = text == null ? "" : text;
        String normalizedText = normalize(safeText);

        if (!normalizedText.isBlank() && containsBlockedTerm(normalizedText)) {
            return AutomaticModerationDecision.flagged(ReviewFlagReason.FOUT_TAALGEBRUIK);
        }

        if (containsObfuscatedBlockedTerm(safeText)) {
            return AutomaticModerationDecision.flagged(ReviewFlagReason.FOUT_TAALGEBRUIK);
        }

        if (isLinkSpam(safeText) || hasLongCharacterSpam(safeText)) {
            return AutomaticModerationDecision.flagged(ReviewFlagReason.SPAM);
        }

        return AutomaticModerationDecision.clean();
    }
    /**
     * @param normalizedText genormaliseerde tekst (lowercase, zonder speciale tekens)
     * @return true als de tekst een exact geblokkeerd woord bevat
     */
    private boolean containsBlockedTerm(String normalizedText) {
        if (blockedTerms.isEmpty()) {
            return false;
        }

        String wrappedText = " " + normalizedText + " ";
        for (String term : blockedTerms) {
            if (wrappedText.contains(" " + term + " ")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Controleert of een reviewtekst te veel links bevat om als spam beschouwd te worden.
     * Herkent zowel http(s):// links als www. links via een regex-patroon.
     *
     * Stopt met tellen zodra de drempel bereikt is (vroege exit) om onnodige
     * verwerking van lange teksten te vermijden.
     *
     * @param text de te controleren reviewtekst, null wordt behandeld als lege string
     * @return true als het aantal gevonden links groter of gelijk is aan maxLinksInReview
     */
    private boolean isLinkSpam(String text) {
        Matcher matcher = URL_PATTERN.matcher(text == null ? "" : text);
        int links = 0;

        while (matcher.find()) {
            links++;
            if (links >= maxLinksInReview) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detecteert leetspeak-varianten van geblokkeerde woorden.
     * Zo wordt bv. '@' herkend als 'a' en '3' als 'e'.
     *
     * @param text de originele (niet-genormaliseerde) reviewtekst
     * @return true als een obfuscatiepatroon matcht
     */
    private boolean containsObfuscatedBlockedTerm(String text) {
        if (text == null || text.isBlank() || blockedTermObfuscationPatterns.isEmpty()) {
            return false;
        }

        for (Pattern pattern : blockedTermObfuscationPatterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param text de reviewtekst
     * @return true als hetzelfde karakter 10 of meer keer na elkaar voorkomt
     */
    private boolean hasLongCharacterSpam(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        return REPEATED_CHARACTER_PATTERN.matcher(text).find();
    }

    /**
     * Normaliseert tekst voor vergelijking: converteert naar lowercase,
     * verwijdert niet-alfanumerieke tekens en reduceert witruimte.
     *
     * @param value de te normaliseren tekst
     * @return genormaliseerde tekst
     */
    private String normalize(String value) {
        return MULTI_SPACE_PATTERN.matcher(NON_ALPHANUMERIC_PATTERN.matcher(value.toLowerCase()).replaceAll(" "))
                .replaceAll(" ")
                .trim();
    }

    /**
     * Bouwt een regex-patroon dat leetspeak-varianten van een geblokkeerd woord herkent.
     * Retourneert null voor woorden korter dan 3 tekens of woorden met spaties.
     *
     * @param normalizedBlockedTerm genormaliseerd geblokkeerd woord
     * @return compiled Pattern of null als het woord niet geschikt is voor obfuscatiedetectie
     */
    private Pattern buildObfuscationPattern(String normalizedBlockedTerm) {
        if (normalizedBlockedTerm.contains(" ")) {
            return null;
        }

        String compactTerm = normalizedBlockedTerm.replace(" ", "");
        if (compactTerm.length() < 3) {
            return null;
        }

        StringBuilder regexBuilder = new StringBuilder("(?iu)");
        for (int i = 0; i < compactTerm.length(); i++) {
            char ch = compactTerm.charAt(i);
            regexBuilder.append(letterToClass(ch));
            if (i < compactTerm.length() - 1) {
                regexBuilder.append("[\\W_]*");
            }
        }

        return Pattern.compile(regexBuilder.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private String letterToClass(char ch) {
        String mappedClass = LETTER_CLASS_MAP.get(ch);
        if (mappedClass != null) {
            return mappedClass;
        }

        return Pattern.quote(String.valueOf(ch));
    }


    /**
     * Bouwt de mapping van letters naar hun leetspeak-equivalenten.
     * Bijvoorbeeld: 'a' → [@4], 'e' → [e3], 'i' → [i1!|].
     *
     * @return onveranderlijke map van letter naar regex-karakterklasse
     */
    private static Map<Character, String> buildLetterClassMap() {
        Map<Character, String> map = new HashMap<>();
        map.put('a', "[aA@4]");
        map.put('b', "[bB8]");
        map.put('e', "[eE3]");
        map.put('g', "[gG69]");
        map.put('i', "[iI1!|]");
        map.put('l', "[lL1|]");
        map.put('o', "[oO0]");
        map.put('s', "[sS$5]");
        map.put('t', "[tT7+]");
        map.put('u', "[uU#vV]");
        map.put('z', "[zZ2]");
        return Map.copyOf(map);
    }
}
