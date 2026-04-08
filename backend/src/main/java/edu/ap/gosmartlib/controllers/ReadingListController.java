package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListResponseDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.services.ReadingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reading-lists")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;

    @PostMapping
    public ResponseEntity<ReadingListEntity> createReadingList(@RequestBody CreateReadingListDTO dto) {
        return ResponseEntity.ok(readingListService.createReadingList(dto));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReadingListResponseDTO>> getListsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(readingListService.getListsByCreatorId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReadingListResponseDTO> getListById(@PathVariable Long id) {
        return ResponseEntity.ok(readingListService.getListById(id));
    }

    @PutMapping("/{id}/books")
    public ResponseEntity<ReadingListResponseDTO> updateBooksInList(@PathVariable Long id, @RequestBody List<Long> bookIds) {
        // Nu geeft deze ook netjes een DTO terug na het opslaan!
        return ResponseEntity.ok(readingListService.syncBooksInList(id, bookIds));
    }
}