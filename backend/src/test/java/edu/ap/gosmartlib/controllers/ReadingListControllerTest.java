package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.security.AuthHelper;
import edu.ap.gosmartlib.services.ReadingListService;
import edu.ap.gosmartlib.util.ReadingListType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingListControllerTest {

    @Mock
    private ReadingListService readingListService;

    @Mock
    private OAuth2User oauth2User;

    // @Spy gebruikt de echte implementatie van AuthHelper zodat extractUid/extractUidOrNull
    // correct werken zonder elke test afzonderlijk te stubben.
    @Spy
    private AuthHelper authHelper = new AuthHelper();

    @InjectMocks
    private ReadingListController readingListController;

    @Test
    void givenValidAuthentication_whenGetVisibleLists_thenReturnsOkWithBody() {
        String uid = "user-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ReadingListOverviewDTO list = new ReadingListOverviewDTO(
                1L,
                "List",
                "Task",
                null,
                ReadingListType.PERSONAL,
                true,
                "user-1",
                List.of(10L),
                1
        );
        when(readingListService.getVisibleLists(uid)).thenReturn(List.of(list));

        ResponseEntity<?> response = readingListController.getVisibleLists(oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(readingListService, times(1)).getVisibleLists(uid);
    }

    @Test
    void givenMissingAuthentication_whenGetVisibleLists_thenThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListController.getVisibleLists(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(readingListService, never()).getVisibleLists(any());
    }

    @Test
    void givenServiceThrows_whenGetVisibleLists_thenPropagatesException() {
        String uid = "user-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);
        when(readingListService.getVisibleLists(uid)).thenThrow(new IllegalArgumentException("boom"));

        assertThrows(IllegalArgumentException.class,
                () -> readingListController.getVisibleLists(oauth2User));
    }

    @Test
    void givenValidAuthentication_whenGetListDetail_thenReturnsOk() {
        String uid = "user-2";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ReadingListDetailDTO detail = new ReadingListDetailDTO(
                2L,
                "Detail",
                "Task",
                null,
                ReadingListType.CLASS,
                false,
                "teacher",
                List.of(new ReadingListDetailDTO.BookItem(10L, "Book", List.of("Author"), null, "isbn-10", 0))
        );
        when(readingListService.getListDetail(2L, uid)).thenReturn(detail);

        ResponseEntity<?> response = readingListController.getListDetail(2L, oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detail, response.getBody());
        verify(readingListService, times(1)).getListDetail(2L, uid);
    }

    @Test
    void givenValidAuthentication_whenCreateClassList_thenReturnsCreatedId() {
        String uid = "teacher-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity created = new ReadingListEntity();
        created.setId(10L);
        when(readingListService.createClassList(dto, uid)).thenReturn(created);

        ResponseEntity<?> response = readingListController.createClassList(dto, oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenCreatePersonalList_thenReturnsCreatedId() {
        String uid = "student-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity created = new ReadingListEntity();
        created.setId(11L);
        when(readingListService.createPersonalList(dto, uid)).thenReturn(created);

        ResponseEntity<?> response = readingListController.createPersonalList(dto, oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenUpdatePersonalList_thenReturnsUpdatedId() {
        String uid = "student-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity updated = new ReadingListEntity();
        updated.setId(12L);
        when(readingListService.updatePersonalList(12L, dto, uid)).thenReturn(updated);

        ResponseEntity<?> response = readingListController.updatePersonalList(12L, dto, oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(12L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenDeletePersonalList_thenReturnsNoContent() {
        String uid = "student-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ResponseEntity<?> response = readingListController.deletePersonalList(13L, oauth2User);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(readingListService, times(1)).deletePersonalList(13L, uid);
    }

    @Test
    void givenValidAuthentication_whenDeleteClassList_thenReturnsNoContent() {
        String uid = "teacher-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ResponseEntity<?> response = readingListController.deleteClassList(14L, oauth2User);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(readingListService, times(1)).deleteClassList(14L, uid);
    }

    @Test
    void givenValidAuthentication_whenUpdateClassList_thenReturnsUpdatedId() {
        String uid = "teacher-1";
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity updated = new ReadingListEntity();
        updated.setId(15L);
        when(readingListService.updateClassList(15L, dto, uid)).thenReturn(updated);

        ResponseEntity<?> response = readingListController.updateClassList(15L, dto, oauth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(15L, response.getBody());
        verify(readingListService, times(1)).updateClassList(15L, dto, uid);
    }

    @Test
    void givenOAuthUserWithoutUserId_whenGetVisibleLists_thenThrowsUnauthorized() {
        when(oauth2User.getAttribute("userID")).thenReturn("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingListController.getVisibleLists(oauth2User));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(readingListService, never()).getVisibleLists(any());
    }
}
