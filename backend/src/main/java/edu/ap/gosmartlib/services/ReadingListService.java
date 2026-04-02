package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.entities.BookEntity;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.repositories.BookRepository;
import edu.ap.gosmartlib.repositories.ReadingListRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public ReadingListEntity createReadingList(CreateReadingListDTO dto) {
        
        // We zoeken nu gewoon op het normale database ID!
        UserEntity creator = userRepository.findById(dto.getCreatorId())
                .orElseThrow(() -> new RuntimeException("Gebruiker niet gevonden in DB met ID: " + dto.getCreatorId()));

        ReadingListEntity readingList = new ReadingListEntity();
        readingList.setTitle(dto.getTitle());
        readingList.setTaskDescription(dto.getTaskDescription());
        readingList.setDeadline(LocalDateTime.parse(dto.getDeadline())); 
        readingList.setCreator(creator);

        if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {
            List<BookEntity> books = bookRepository.findAllById(dto.getBookIds());
            readingList.getBooks().addAll(books);
        }

        return readingListRepository.save(readingList);
    }
}