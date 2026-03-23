package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.entities.SchoolClassEntity;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/smartschool")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SmartschoolController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    @Transactional(readOnly = true) // Nodig omdat we de klassen ophalen die Lazy ingeladen worden
    public ResponseEntity<List<Map<String, String>>> searchSmartschoolUsers(@RequestParam String query) {
        
        // We zoeken alle gebruikers op in de database en filteren op smartschoolUid
        // omdat de UserEntity geen aparte velden voor de naam of achternaam opslaat.
        List<Map<String, String>> filteredResults = userRepository.findAll().stream()
            .filter(u -> u.getSmartschoolUid().toLowerCase().contains(query.toLowerCase()))
            .map(u -> {
                // Haal de eerste klasgroep op (indien de gebruiker aan een klas is gekoppeld)
                String classGroup = u.getClasses().stream()
                        .findFirst()
                        .map(SchoolClassEntity::getName)
                        .orElse("Onbekend");

                // Haal de school op
                String schoolName = u.getSchool() != null && u.getSchool().getName() != null 
                                    ? u.getSchool().getName() : "Onbekend";
                String schoolId = u.getSchool() != null && u.getSchool().getId() != null 
                                    ? String.valueOf(u.getSchool().getId()) : "";

                // Map.of staat geen null waarden toe, dus we zorgen voor een default ("Onbekend" of "")
                return Map.of(
                    "smartschoolUserId", u.getSmartschoolUid(),
                    "name", u.getSmartschoolUid(), // Gebruik het ID als weergavenaam in React
                    "classGroup", classGroup,
                    "school", schoolName,
                    "schoolId", schoolId,
                    "photoUrl", "https://ui-avatars.com/api/?name=" + u.getSmartschoolUid() + "&background=random"
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(filteredResults);
    }
}