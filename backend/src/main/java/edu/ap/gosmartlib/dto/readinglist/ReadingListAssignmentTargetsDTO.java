package edu.ap.gosmartlib.dto.readinglist;

import java.util.List;

/**
 * Beschikbare doelgroepen voor het aanmaken of aanpassen van een klasleeslijst.
 */
public record ReadingListAssignmentTargetsDTO(
                List<StudentTarget> students,
                List<ClassTarget> classes,
                List<Integer> years,
                List<Integer> grades) {
        public record StudentTarget(
                        Long id,
                        String displayName,
                        List<String> classNames) {
        }

        public record ClassTarget(
                        Long id,
                        String name,
                        Integer year,
                        Integer grade) {
        }
}