package edu.ap.gosmartlib.dto.schoolIntegration.schoolCampus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolCampusRequest(
                @NotBlank(message = "Campusnaam is verplicht") @Size(max = 120, message = "Campusnaam mag maximaal 120 tekens lang zijn") String name) {
}