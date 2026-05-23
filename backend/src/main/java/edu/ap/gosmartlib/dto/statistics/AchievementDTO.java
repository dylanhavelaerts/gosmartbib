package edu.ap.gosmartlib.dto.statistics;

public record AchievementDTO(
        String categoryKey,
        String categoryLabel,
        String currentTier,
        int currentValue,
        int nextThreshold,
        String nextTier
) {}
