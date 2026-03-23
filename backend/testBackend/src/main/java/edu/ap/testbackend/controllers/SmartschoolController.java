package edu.ap.testbackend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/smartschool")
@CrossOrigin(origins = "*")
public class SmartschoolController {

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, String>>> searchSmartschoolUsers(@RequestParam String query) {
        
        
        List<Map<String, String>> mockUsers = List.of(
            Map.of(
                "smartschoolUserId", "SS-12345", 
                "name", "Jan Peeters", 
                "classGroup", "5IT", 
                "photoUrl", "https://ui-avatars.com/api/?name=Jan+Peeters&background=random"
            ),
            Map.of(
                "smartschoolUserId", "SS-67890", 
                "name", "Janssen Peter", 
                "classGroup", "6B", 
                "photoUrl", "https://ui-avatars.com/api/?name=Janssen+Peter&background=random"
            ),
            Map.of(
                "smartschoolUserId", "SS-11111", 
                "name", "Marie Claes", 
                "classGroup", "5IT", 
                "photoUrl", "https://ui-avatars.com/api/?name=Marie+Claes&background=random"
            )
        );

        // Filter the data based on the partial name query
        List<Map<String, String>> filteredResults = mockUsers.stream()
            .filter(u -> u.get("name").toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(filteredResults);
    }
}