package edu.ap.gosmartlib.controllers.book;

import edu.ap.gosmartlib.dto.lessontip.CreateLessonTipDTO;
import edu.ap.gosmartlib.dto.lessontip.LessonTipDTO;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.LessonTipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonTipControllerTest {

    @Mock private LessonTipService lessonTipService;
    @Mock private AuthHelper authHelper;
    @Mock private OAuth2User principal;

    @InjectMocks
    private LessonTipController lessonTipController;

    private static final String UID = "teacher-uid";

    @Test
    void givenValidPrincipal_whenGetByBook_thenReturnsOkWithList() {
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(lessonTipService.getByBookId(1L, UID)).thenReturn(List.of());

        ResponseEntity<List<LessonTipDTO>> response = lessonTipController.getByBook(1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(lessonTipService).getByBookId(1L, UID);
    }

    @Test
    void givenValidPrincipal_whenCreate_thenReturnsCreated() {
        CreateLessonTipDTO dto = mock(CreateLessonTipDTO.class);
        LessonTipDTO created = mock(LessonTipDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(lessonTipService.create(2L, UID, dto)).thenReturn(created);

        ResponseEntity<LessonTipDTO> response = lessonTipController.create(2L, dto, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(created, response.getBody());
        verify(lessonTipService).create(2L, UID, dto);
    }

    @Test
    void givenValidPrincipal_whenUpdate_thenReturnsOkWithUpdated() {
        CreateLessonTipDTO dto = mock(CreateLessonTipDTO.class);
        LessonTipDTO updated = mock(LessonTipDTO.class);
        when(authHelper.extractUid(principal)).thenReturn(UID);
        when(lessonTipService.update(10L, UID, dto)).thenReturn(updated);

        ResponseEntity<LessonTipDTO> response = lessonTipController.update(10L, dto, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
        verify(lessonTipService).update(10L, UID, dto);
    }

    @Test
    void givenValidPrincipal_whenDelete_thenReturnsNoContent() {
        when(authHelper.extractUid(principal)).thenReturn(UID);

        ResponseEntity<Void> response = lessonTipController.delete(5L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(lessonTipService).delete(5L, UID);
    }
}
