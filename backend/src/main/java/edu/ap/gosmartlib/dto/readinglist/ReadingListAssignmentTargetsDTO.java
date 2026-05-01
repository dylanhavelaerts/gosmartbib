package edu.ap.gosmartlib.dto.readinglist;

import java.util.List;

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