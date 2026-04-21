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

@Service
public class ReviewAutoModerationService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHARACTER_PATTERN = Pattern.compile("(.)\\1{9,}");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
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

    private boolean hasLongCharacterSpam(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        return REPEATED_CHARACTER_PATTERN.matcher(text).find();
    }

    private String normalize(String value) {
        return MULTI_SPACE_PATTERN.matcher(NON_ALPHANUMERIC_PATTERN.matcher(value.toLowerCase()).replaceAll(" "))
                .replaceAll(" ")
                .trim();
    }

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
