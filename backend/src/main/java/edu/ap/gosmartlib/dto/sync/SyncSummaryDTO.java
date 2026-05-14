package edu.ap.gosmartlib.dto.sync;

import java.util.List;

public record SyncSummaryDTO(int totalAdded, int totalRemoved, List<SchoolSyncResultDTO> schools) {
    public boolean hasErrors() {
        return schools.stream().anyMatch(SchoolSyncResultDTO::hasErrors);
    }
}
