package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListResponseDTO;
import edu.ap.gosmartlib.dto.BookDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public ReadingListEntity createReadingList(CreateReadingListDTO dto) {
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

    @Transactional(readOnly = true)
    public List<ReadingListResponseDTO> getListsByCreatorId(Long creatorId) {
        List<ReadingListEntity> entities = readingListRepository.findByCreatorId(creatorId);
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReadingListResponseDTO getListById(Long id) {
        ReadingListEntity entity = readingListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leeslijst niet gevonden in DB met ID: " + id));
        return convertToDTO(entity);
    }

    @Transactional
    public ReadingListResponseDTO syncBooksInList(Long listId, List<Long> bookIds) {
        ReadingListEntity readingList = readingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Leeslijst niet gevonden in DB met ID: " + listId));
        
        List<BookEntity> selectedBooks = bookRepository.findAllById(bookIds);

        readingList.getBooks().clear();
        readingList.getBooks().addAll(selectedBooks);

        // Opslaan en direct vertalen naar een veilige DTO
        ReadingListEntity savedEntity = readingListRepository.save(readingList);
        return convertToDTO(savedEntity);
    }

    // --- DE VERTALER ---
    private ReadingListResponseDTO convertToDTO(ReadingListEntity entity) {
        ReadingListResponseDTO dto = new ReadingListResponseDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setTaskDescription(entity.getTaskDescription());
        dto.setDeadline(entity.getDeadline());

        List<BookDTO> bookDTOs = entity.getBooks().stream().map(book -> {
            return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthors() != null ? new ArrayList<>(book.getAuthors()) : new ArrayList<>(),
                book.getPublisher(),
                book.getDescription(),
                book.getPageCount(),
                book.getCategories() != null ? new ArrayList<>(book.getCategories()) : new ArrayList<>(),
                book.getThumbnail(),
                book.getLanguage(),
                book.getRating(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.isDidacticTag(),
                book.getLabels() != null ? new ArrayList<>(book.getLabels()) : new ArrayList<>(),
                book.getReadingLevel(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getAgeRange()
            );
        }).collect(Collectors.toList());

        dto.setBooks(bookDTOs);
        return dto;
    }
}