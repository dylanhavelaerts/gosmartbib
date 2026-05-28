package edu.ap.gosmartlib.dto.statistics;

public record ReaderProfileDTO(
        String profileType,   // "AVONTURIER" | "PIONIER" | "SPRINTER" | "TITAN" | null
        String profileLabel,
        int booksRead,
        int booksNeeded // 0 - 5,  5 boeken nodig voor je iets ziet
) {}
