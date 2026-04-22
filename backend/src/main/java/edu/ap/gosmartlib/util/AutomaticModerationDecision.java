package edu.ap.gosmartlib.util;

public record AutomaticModerationDecision(boolean flagged, ReviewFlagReason reason) {

    public static AutomaticModerationDecision clean() {
        return new AutomaticModerationDecision(false, null);
    }

    public static AutomaticModerationDecision flagged(ReviewFlagReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Een flagged beslissing moet een reden bevatten");
        }

        return new AutomaticModerationDecision(true, reason);
    }
}
