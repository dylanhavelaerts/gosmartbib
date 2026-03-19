package edu.ap.gosmartlib.dto.importdto;

import java.util.List;

public record BulkImportResponseDTO(
                int totalRows,
                int savedCount,
                int mismatchCount,
                List<ImportMismatchDTO> mismatches) {
}
