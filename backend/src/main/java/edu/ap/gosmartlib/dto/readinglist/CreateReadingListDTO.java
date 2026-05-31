package edu.ap.gosmartlib.dto.readinglist;

import edu.ap.gosmartlib.util.ReadingListTargetType;
import lombok.Data;
import java.util.List;

/**
 * Inkomende payload voor het aanmaken of aanpassen van een leeslijst.
 *
 * <p>
 * Dezelfde DTO wordt gebruikt voor persoonlijke leeslijsten en klasleeslijsten.
 * Bij persoonlijke leeslijsten worden de doelgroepvelden genegeerd. Bij
 * klasleeslijsten
 * bepalen targetType en de bijhorende targetvelden voor welke leerlingen de
 * lijst
 * zichtbaar wordt.
 * </p>
 */
@Data
public class CreateReadingListDTO {
    private String title;
    private String taskDescription;
    private String deadline;
    private List<Long> bookIds;
    private ReadingListTargetType targetType;
    private List<Long> targetStudentIds;
    private List<Long> targetClassIds;
    private List<Integer> targetYears;
    private List<Integer> targetGrades;
    private Boolean targetAllSchools;
}