package edu.ap.gosmartlib.dto.importdto;

import java.util.List;

public record BulkImportDuplicateWarningDTO(
                int rowNumber,
                Long existingBookId,
                String title,
                List<String> authors,
                String publisher,
                String campus,
                int totalCopiesToAdd,
                int availableCopiesToAdd,
                int currentTotalCopies,
                int currentAvailableCopies,
                String reason) {
}