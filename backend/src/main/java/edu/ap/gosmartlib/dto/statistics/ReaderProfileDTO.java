package edu.ap.gosmartlib.dto.statistics;

public record ReaderProfileDTO(
        String profileType,   // "AVONTURIER" | "PIONIER" | "SPRINTER" | "TITAN" | null
        String profileLabel,  // Dutch display name | null
        int booksRead,
        int booksNeeded       // 0 once unlocked, (5 - booksRead) when still locked
) {}
