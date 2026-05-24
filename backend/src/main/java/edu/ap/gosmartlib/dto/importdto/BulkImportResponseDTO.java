package edu.ap.gosmartlib.dto.importdto;

import java.util.List;

public record BulkImportResponseDTO(
        int totalRows,
        int savedCount,
        int mismatchCount,
        List<ImportMismatchDTO> mismatches,
        int duplicateWarningCount,
        List<BulkImportDuplicateWarningDTO> duplicateWarnings) {
    public BulkImportResponseDTO(
            int totalRows,
            int savedCount,
            int mismatchCount,
            List<ImportMismatchDTO> mismatches) {
        this(totalRows, savedCount, mismatchCount, mismatches, 0, List.of());
    }
}