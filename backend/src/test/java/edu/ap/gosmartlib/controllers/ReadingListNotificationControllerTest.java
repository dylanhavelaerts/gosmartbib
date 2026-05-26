package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.config.TestSecurityConfig;
import edu.ap.gosmartlib.services.ReadingListService;
import edu.ap.gosmartlib.services.messages.BookNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ReadingListNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private BookNotificationService bookNotificationService;
    @MockitoBean private ReadingListService readingListService;

    // ─── GET /reading-lists/{id}/notification ─────────────────────────────────

    @Test
    void givenAllNotificationsEnabled_whenStatus_thenReturnsTrue() throws Exception {
        when(readingListService.getBookIds(1L)).thenReturn(List.of(10L, 11L));
        when(bookNotificationService.isAllEnabled("uid-1", List.of(10L, 11L))).thenReturn(true);

        mockMvc.perform(get("/reading-lists/1/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void givenNotAllNotificationsEnabled_whenStatus_thenReturnsFalse() throws Exception {
        when(readingListService.getBookIds(1L)).thenReturn(List.of(10L, 11L));
        when(bookNotificationService.isAllEnabled("uid-1", List.of(10L, 11L))).thenReturn(false);

        mockMvc.perform(get("/reading-lists/1/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void givenReadingListNotFound_whenStatus_thenReturnsNotFound() throws Exception {
        when(readingListService.getBookIds(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/reading-lists/99/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNullPrincipal_whenStatus_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/reading-lists/1/notification"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookNotificationService, readingListService);
    }

    @Test
    void givenPrincipalWithoutUserId_whenStatus_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/reading-lists/1/notification")
                        .with(oauth2Login()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookNotificationService, readingListService);
    }

    // ─── POST /reading-lists/{id}/notification ────────────────────────────────

    @Test
    void givenValidPrincipalAndList_whenEnable_thenReturnsOk() throws Exception {
        when(readingListService.getBookIds(2L)).thenReturn(List.of(20L, 21L));

        mockMvc.perform(post("/reading-lists/2/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isOk());

        verify(bookNotificationService).enableBulk("uid-1", List.of(20L, 21L));
    }

    @Test
    void givenReadingListNotFound_whenEnable_thenReturnsNotFound() throws Exception {
        when(readingListService.getBookIds(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/reading-lists/99/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isNotFound());

        verify(bookNotificationService, never()).enableBulk(any(), any());
    }

    @Test
    void givenNullPrincipal_whenEnable_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/reading-lists/2/notification"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookNotificationService, readingListService);
    }

    @Test
    void givenPrincipalWithoutUserId_whenEnable_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/reading-lists/2/notification")
                        .with(oauth2Login()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookNotificationService, readingListService);
    }

    // ─── DELETE /reading-lists/{id}/notification ──────────────────────────────

    @Test
    void givenValidPrincipalAndList_whenDisable_thenReturnsNoContent() throws Exception {
        when(readingListService.getBookIds(3L)).thenReturn(List.of(30L));

        mockMvc.perform(delete("/reading-lists/3/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isNoContent());

        verify(bookNotificationService).disableBulk("uid-1", List.of(30L));
    }

    @Test
    void givenReadingListNotFound_whenDisable_thenReturnsNotFound() throws Exception {
        when(readingListService.getBookIds(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(delete("/reading-lists/99/notification")
                        .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                .andExpect(status().isNotFound());

        verify(bookNotificationService, never()).disableBulk(any(), any());
    }

    @Test
    void givenNullPrincipal_whenDisable_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/reading-lists/3/notification"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookNotificationService, readingListService);
    }
}
