package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.readinglist.CreateReadingListDTO;
import edu.ap.gosmartlib.dto.readinglist.PublicReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListBookDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListDetailDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListOverviewDTO;
import edu.ap.gosmartlib.dto.readinglist.ReadingListVisibilityDTO;
import edu.ap.gosmartlib.dto.readinglist.UpdateReadingListVisibilityDTO;
import edu.ap.gosmartlib.entities.ReadingListEntity;
import edu.ap.gosmartlib.services.ReadingListService;
import edu.ap.gosmartlib.util.ReadingListType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @InjectMocks
    private ReadingListController readingListController;

    @Test
    void givenValidAuthentication_whenGetVisibleLists_thenReturnsOkWithBody() {
        String uid = "user-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ReadingListOverviewDTO list = new ReadingListOverviewDTO(
                1L,
                "public-uid-1",
                "List",
                "Task",
                null,
                "user-1",
                1,
                ReadingListType.PERSONAL,
                true,
                false,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false);
        when(readingListService.getVisibleLists(uid)).thenReturn(List.of(list));

        ResponseEntity<?> response = readingListController.getVisibleLists(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(readingListService, times(1)).getVisibleLists(uid);
    }

    @Test
    void givenMissingAuthentication_whenGetVisibleLists_thenReturnsBadRequest() {
        ResponseEntity<?> response = readingListController.getVisibleLists(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Could not load reading lists: Not authenticated", response.getBody());
        verify(readingListService, never()).getVisibleLists(any());
    }

    @Test
    void givenServiceThrows_whenGetVisibleLists_thenReturnsBadRequest() {
        String uid = "user-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);
        when(readingListService.getVisibleLists(uid)).thenThrow(new IllegalArgumentException("boom"));

        ResponseEntity<?> response = readingListController.getVisibleLists(authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Could not load reading lists: boom", response.getBody());
    }

    @Test
    void givenValidAuthentication_whenGetListDetail_thenReturnsOk() {
        String uid = "user-2";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ReadingListDetailDTO detail = new ReadingListDetailDTO(
                2L,
                "public-uid-2",
                "Detail",
                "Task",
                null,
                ReadingListType.CLASS,
                false,
                false,
                "teacher",
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                List.of(new ReadingListBookDTO(10L, "Book", List.of("Author"), null, "isbn-10", 0)));
        when(readingListService.getListDetail(2L, uid)).thenReturn(detail);

        ResponseEntity<?> response = readingListController.getListDetail(2L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detail, response.getBody());
        verify(readingListService, times(1)).getListDetail(2L, uid);
    }

    @Test
    void givenValidAuthentication_whenCreateClassList_thenReturnsCreatedId() {
        String uid = "teacher-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity created = new ReadingListEntity();
        created.setId(10L);
        when(readingListService.createClassList(dto, uid)).thenReturn(created);

        ResponseEntity<?> response = readingListController.createClassList(dto, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenCreatePersonalList_thenReturnsCreatedId() {
        String uid = "student-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity created = new ReadingListEntity();
        created.setId(11L);
        when(readingListService.createPersonalList(dto, uid)).thenReturn(created);

        ResponseEntity<?> response = readingListController.createPersonalList(dto, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenUpdatePersonalList_thenReturnsUpdatedId() {
        String uid = "student-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity updated = new ReadingListEntity();
        updated.setId(12L);
        when(readingListService.updatePersonalList(12L, dto, uid)).thenReturn(updated);

        ResponseEntity<?> response = readingListController.updatePersonalList(12L, dto, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(12L, response.getBody());
    }

    @Test
    void givenValidAuthentication_whenDeletePersonalList_thenReturnsNoContent() {
        String uid = "student-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ResponseEntity<?> response = readingListController.deletePersonalList(13L, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(readingListService, times(1)).deletePersonalList(13L, uid);
    }

    @Test
    void givenValidAuthentication_whenDeleteClassList_thenReturnsNoContent() {
        String uid = "teacher-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        ResponseEntity<?> response = readingListController.deleteClassList(14L, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(readingListService, times(1)).deleteClassList(14L, uid);
    }

    @Test
    void givenValidAuthentication_whenUpdateClassList_thenReturnsUpdatedId() {
        String uid = "teacher-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        CreateReadingListDTO dto = new CreateReadingListDTO();
        ReadingListEntity updated = new ReadingListEntity();
        updated.setId(15L);
        when(readingListService.updateClassList(15L, dto, uid)).thenReturn(updated);

        ResponseEntity<?> response = readingListController.updateClassList(15L, dto, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(15L, response.getBody());
        verify(readingListService, times(1)).updateClassList(15L, dto, uid);
    }

    @Test
    void givenPublicUid_whenGetPublicListDetail_thenReturnsOk() {
        String publicUid = "public-uid-3";

        PublicReadingListDetailDTO detail = new PublicReadingListDetailDTO(
                publicUid,
                "Shared list",
                "Shared task",
                null,
                "STUDENT",
                List.of(new ReadingListBookDTO(
                        20L,
                        "Shared book",
                        List.of("Author"),
                        null,
                        "isbn-20",
                        1)));

        when(readingListService.getPublicListDetail(publicUid)).thenReturn(detail);

        ResponseEntity<?> response = readingListController.getPublicListDetail(publicUid);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detail, response.getBody());
        verify(readingListService, times(1)).getPublicListDetail(publicUid);
    }

    @Test
    void givenUnknownPublicUid_whenGetPublicListDetail_thenReturnsNotFound() {
        String publicUid = "missing-public-uid";

        when(readingListService.getPublicListDetail(publicUid))
                .thenThrow(new IllegalArgumentException("Publieke leeslijst niet gevonden"));

        ResponseEntity<?> response = readingListController.getPublicListDetail(publicUid);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Publieke leeslijst niet gevonden", response.getBody());
        verify(readingListService, times(1)).getPublicListDetail(publicUid);
    }

    @Test
    void givenValidAuthentication_whenUpdatePersonalListVisibility_thenReturnsUpdatedVisibility() {
        String uid = "student-1";
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn(uid);

        UpdateReadingListVisibilityDTO dto = new UpdateReadingListVisibilityDTO(true);

        ReadingListVisibilityDTO updated = new ReadingListVisibilityDTO(
                12L,
                "public-uid-12",
                true);

        when(readingListService.updatePersonalListVisibility(12L, true, uid)).thenReturn(updated);

        ResponseEntity<?> response = readingListController.updatePersonalListVisibility(12L, dto, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
        verify(readingListService, times(1)).updatePersonalListVisibility(12L, true, uid);
    }

    @Test
    void givenOAuthUserWithoutUserId_whenGetVisibleLists_thenReturnsBadRequest() {
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("userID")).thenReturn("   ");

        ResponseEntity<?> response = readingListController.getVisibleLists(authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Could not load reading lists: Authenticated user ID not found", response.getBody());
        verify(readingListService, never()).getVisibleLists(any());
    }
}
