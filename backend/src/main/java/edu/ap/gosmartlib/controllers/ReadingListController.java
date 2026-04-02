package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.services.ReadingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reading-lists")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;

    @PostMapping
    public ResponseEntity<?> createReadingList(@RequestBody CreateReadingListDTO dto) {
        try {
            readingListService.createReadingList(dto);
            return ResponseEntity.ok().body("Leeslijst succesvol aangemaakt!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fout bij aanmaken leeslijst: " + e.getMessage());
        }
    }
}