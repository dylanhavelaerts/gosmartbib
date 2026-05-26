package edu.ap.gosmartlib.services;

import edu.ap.gosmartlib.dto.lessontip.CreateLessonTipDTO;
import edu.ap.gosmartlib.dto.lessontip.LessonTipDTO;
import edu.ap.gosmartlib.dto.userdirectory.ResolveDisplayNamesRequest;
import edu.ap.gosmartlib.entities.book.BookEntity;
import edu.ap.gosmartlib.entities.LessonTipEntity;
import edu.ap.gosmartlib.entities.UserEntity;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.repositories.book.BookRepository;
import edu.ap.gosmartlib.repositories.LessonTipRepository;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.users.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonTipService {
    private final LessonTipRepository lessonTipRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final UserDirectoryService userDirectoryService;

    @Transactional(readOnly = true)
    public List<LessonTipDTO> getByBookId(Long bookId, String actorUid) {
        List<LessonTipEntity> tips = lessonTipRepository.findByBook_IdOrderByCreatedDateDesc(bookId);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, tips);
        return tips.stream().map(tip -> toDTO(tip, actorUid, displayNames)).toList();
    }


    public LessonTipDTO create(Long bookId, String actorUid, CreateLessonTipDTO dto) {
        UserEntity user = requireUser(actorUid);
        BookEntity book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        LessonTipEntity tip = new LessonTipEntity();
        tip.setBook(book);
        tip.setUser(user);
        tip.setText(dto.text() == null ? "" : dto.text().trim());
        tip.setAnonymous(dto.anonymous());
        tip.setCreatedDate(LocalDate.now());

        LessonTipEntity saved = lessonTipRepository.save(tip);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, List.of(saved));
        return toDTO(saved, actorUid, displayNames);
    }

    @Transactional
    public LessonTipDTO update(Long tipId, String actorUid, CreateLessonTipDTO dto) {
        UserEntity user = requireUser(actorUid);
        LessonTipEntity tip = requireOwnTip(tipId, user);
        tip.setText(dto.text() == null ? "" : dto.text().trim());
        tip.setAnonymous(dto.anonymous());
        LessonTipEntity saved = lessonTipRepository.save(tip);
        Map<String, String> displayNames = resolveDisplayNamesMap(actorUid, List.of(saved));
        return toDTO(saved, actorUid, displayNames);
    }


    @Transactional
    public void delete(Long tipId, String actorUid) {
        UserEntity user = requireUser(actorUid);
        LessonTipEntity tip = requireOwnTip(tipId, user);
        lessonTipRepository.delete(tip);
    }

    private UserEntity requireUser(String uid) {
        return userRepository.findBySmartschoolUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Gebruiker niet gevonden"));
    }

    private LessonTipEntity requireOwnTip(Long tipId, UserEntity user) {
        LessonTipEntity tip = lessonTipRepository.findById(tipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tip niet gevonden"));
        if (!tip.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Je mag alleen je eigen tips bewerken");
        }
        return tip;
    }

    private Map<String, String> resolveDisplayNamesMap(String actorUid, List<LessonTipEntity> tips) {
        if (actorUid == null || tips == null || tips.isEmpty()) return Map.of();
        List<String> uids = tips.stream()
                .filter(t -> !t.isAnonymous())
                .map(t -> t.getUser().getSmartschoolUid())
                .filter(uid -> uid != null && !uid.isBlank())
                .distinct()
                .toList();
        if (uids.isEmpty()) return Map.of();
        try {
            var response = userDirectoryService.resolveDisplayNames(
                    actorUid, new ResolveDisplayNamesRequest(uids, null));
            if (!response.success() || response.displayNames() == null) return Map.of();
            return response.displayNames();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private LessonTipDTO toDTO(LessonTipEntity tip, String actorUid, Map<String, String> displayNames) {
        String resolvedName = displayNames.get(tip.getUser().getSmartschoolUid());
        String authorName = tip.isAnonymous() ? "Anoniem"
                : (resolvedName != null ? resolvedName : "Leerkracht (andere school)");

        boolean ownTip = actorUid.equals(tip.getUser().getSmartschoolUid());
        return new LessonTipDTO(
                tip.getId(), tip.getText(), tip.isAnonymous(),
                authorName, ownTip, tip.getCreatedDate());
    }
}
