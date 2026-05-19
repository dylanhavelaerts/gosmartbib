package edu.ap.gosmartlib.dto.sync;

import java.util.List;

public record SchoolSyncResultDTO(String schoolDomain, int added, int removed, int classesSynced, int enrollmentsSynced, List<String> errors) {
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
