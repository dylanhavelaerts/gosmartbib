package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.config.TestSecurityConfig;
import edu.ap.gosmartlib.dto.*;
import edu.ap.gosmartlib.dto.importdto.BulkImportResponseDTO;
import edu.ap.gosmartlib.dto.importdto.ImportMismatchDTO;
import edu.ap.gosmartlib.exceptions.BookNotFoundException;
import edu.ap.gosmartlib.services.BookService;
import edu.ap.gosmartlib.services.users.UserService;
import edu.ap.gosmartlib.util.UserRoles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private BookService bookService;

        @MockitoBean
        private UserService userService;

        // ─── helpers ──────────────────────────────────────────────────────────────

        private BookDTO buildDTO(Long id, String title) {
                return new BookDTO(id, title, List.of("Author"), "Publisher", "Description",
                                100, List.of("Category"), "thumbnail", "en", 4.0, "9781234567890",
                                2023, false, null, "A", 1, 1, "Eerste graad",
                                "https://books.google.com/preview", null);
        }

        private void stubAsStudent(String uid) {
                when(userService.getRoleBySmartschoolUid(uid)).thenReturn(UserRoles.STUDENT);
        }

        private void stubAsLibrarian(String uid) {
                when(userService.getRoleBySmartschoolUid(uid)).thenReturn(UserRoles.BIBLIOTHEEKBEHEERDER);
        }

        // ─── GET /books/all ───────────────────────────────────────────────────────

        @Test
        void getBooks_asStudent_returnsPagedBooks() throws Exception {
                stubAsStudent("uid-1");
                when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, "uid-1"))
                                .thenReturn(new PageImpl<>(List.of(buildDTO(1L, "Clean Code")), PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/books/all")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                                .andExpect(jsonPath("$.totalElements").value(1));

                verify(bookService).getAllBooks(0, 20, UserRoles.STUDENT, "uid-1");
        }

        @Test
        void getBooks_withoutLogin_treatsAsStudent() throws Exception {
                when(bookService.getAllBooks(0, 20, UserRoles.STUDENT, null))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                mockMvc.perform(get("/books/all"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void getBooks_serviceThrows_returns500() throws Exception {
                when(bookService.getAllBooks(anyInt(), anyInt(), any(), any()))
                                .thenThrow(new RuntimeException("DB down"));

                mockMvc.perform(get("/books/all"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("Er is een onverwachte fout opgetreden"));
        }

        // ─── GET /books/all/unpaged ───────────────────────────────────────────────

        @Test
        void getAllBooksUnpaged_returnsFullList() throws Exception {
                stubAsStudent("uid-1");
                when(bookService.getAllBooksUnpaged(UserRoles.STUDENT, "uid-1"))
                                .thenReturn(List.of(buildDTO(1L, "Clean Code"), buildDTO(2L, "Effective Java")));

                mockMvc.perform(get("/books/all/unpaged")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        // ─── GET /books/search ────────────────────────────────────────────────────

        @Test
        void search_returnsMatchingBooks() throws Exception {
                stubAsStudent("uid-1");
                when(bookService.searchByTitleOrAuthorOrCategory("Clean", 0, 20, UserRoles.STUDENT, "uid-1"))
                                .thenReturn(new PageImpl<>(List.of(buildDTO(1L, "Clean Code")), PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/books/search")
                                .param("query", "Clean")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
        }

        // ─── GET /books/filter ────────────────────────────────────────────────────

        @Test
        void filterBooks_validRequest_returnsOk() throws Exception {
                when(bookService.filterBooks(any(), eq(UserRoles.STUDENT), isNull()))
                                .thenReturn(new PageImpl<>(List.of(buildDTO(1L, "Clean Code")), PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/books/filter")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk());
        }

        @Test
        void filterBooks_illegalArgument_returns400WithMessage() throws Exception {
                when(bookService.filterBooks(any(), any(), any()))
                                .thenThrow(new IllegalArgumentException(
                                                "minPageCount cannot be bigger than maxPageCount"));

                mockMvc.perform(get("/books/filter")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("minPageCount cannot be bigger than maxPageCount"));
        }

        @Test
        void filterBooks_dataAccessException_returns500WithMessage() throws Exception {
                when(bookService.filterBooks(any(), any(), any()))
                                .thenThrow(new DataAccessException("DB down") {
                                });

                mockMvc.perform(get("/books/filter")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("Er is een databasefout opgetreden"));
        }

        // ─── GET /books/languages, /books/categories, /books/labels ──────────────

        @Test
        void getAvailableLanguages_withLogin_returnsLanguagesForUser() throws Exception {
                when(bookService.getAvailableLanguages("uid-1"))
                                .thenReturn(List.of("en", "nl", "swe"));

                mockMvc.perform(get("/books/languages")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("en"))
                                .andExpect(jsonPath("$[1]").value("nl"))
                                .andExpect(jsonPath("$[2]").value("swe"));

                verify(bookService).getAvailableLanguages("uid-1");
        }

        @Test
        void getAvailableCategories_withLogin_returnsCategoriesForUser() throws Exception {
                when(bookService.getAvailableCategories("uid-1"))
                                .thenReturn(List.of("Fantasy", "Historisch"));

                mockMvc.perform(get("/books/categories")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("Fantasy"))
                                .andExpect(jsonPath("$[1]").value("Historisch"));

                verify(bookService).getAvailableCategories("uid-1");
        }

        @Test
        void getAvailableCategories_withoutLogin_passesNullUid() throws Exception {
                when(bookService.getAvailableCategories(null))
                                .thenReturn(List.of("Fantasy"));

                mockMvc.perform(get("/books/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("Fantasy"));

                verify(bookService).getAvailableCategories(null);
        }

        @Test
        void getAvailableLabels_withLogin_returnsLabelsForUser() throws Exception {
                when(bookService.getAvailableLabels("uid-1"))
                                .thenReturn(List.of("Humor", "Vriendschap"));

                mockMvc.perform(get("/books/labels")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("Humor"))
                                .andExpect(jsonPath("$[1]").value("Vriendschap"));

                verify(bookService).getAvailableLabels("uid-1");
        }

        @Test
        void getAvailableLabels_withoutLogin_passesNullUid() throws Exception {
                when(bookService.getAvailableLabels(null))
                                .thenReturn(List.of("Humor"));

                mockMvc.perform(get("/books/labels"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("Humor"));

                verify(bookService).getAvailableLabels(null);
        }

        // ─── GET /books/{id} ──────────────────────────────────────────────────────

        @Test
        void getBookById_found_returnsOk() throws Exception {
                stubAsStudent("uid-1");
                when(bookService.getBookById(1L, UserRoles.STUDENT, "uid-1"))
                                .thenReturn(buildDTO(1L, "Clean Code"));

                mockMvc.perform(get("/books/1")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Clean Code"));
        }

        @Test
        void getBookById_notFound_returns404() throws Exception {
                stubAsStudent("uid-1");
                when(bookService.getBookById(99L, UserRoles.STUDENT, "uid-1"))
                                .thenThrow(new BookNotFoundException(99L));

                mockMvc.perform(get("/books/99")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-1"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists());
        }

        // ─── GET /books/spotlight ─────────────────────────────────────────────────

        @Test
        void getSpotlight_withoutReadingLevel_callsDefaultMethod() throws Exception {
                when(bookService.getTop4BooksInSpotlight(UserRoles.STUDENT, null))
                                .thenReturn(List.of(buildDTO(1L, "Spotlight Book")));

                mockMvc.perform(get("/books/spotlight"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(bookService).getTop4BooksInSpotlight(UserRoles.STUDENT, null);
                verify(bookService, never()).getTop4BooksInSpotlight(any(), any(), any());
        }

        @Test
        void getSpotlight_withReadingLevel_callsFilteredMethod() throws Exception {
                when(bookService.getTop4BooksInSpotlight(UserRoles.STUDENT, null, "A"))
                                .thenReturn(List.of(buildDTO(1L, "Niveau A Book")));

                mockMvc.perform(get("/books/spotlight")
                                .param("readingLevel", "A"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Niveau A Book"));

                verify(bookService, never()).getTop4BooksInSpotlight(UserRoles.STUDENT, null);
        }

        @Test
        void getSpotlight_blankReadingLevel_callsDefaultMethod() throws Exception {
                when(bookService.getTop4BooksInSpotlight(UserRoles.STUDENT, null))
                                .thenReturn(List.of());

                mockMvc.perform(get("/books/spotlight")
                                .param("readingLevel", "   "))
                                .andExpect(status().isOk());

                verify(bookService).getTop4BooksInSpotlight(UserRoles.STUDENT, null);
        }

        // ─── GET /books/spotlight/all — @PreAuthorize ─────────────────────────────

        @Test
        void getAllSpotlight_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(get("/books/spotlight/all")
                                .with(oauth2Login().attributes(a -> a.put("userID", "uid-student"))))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void getAllSpotlight_asLibrarian_returnsOk() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.getAllBooksInSpotlight(UserRoles.BIBLIOTHEEKBEHEERDER, "uid-lib"))
                                .thenReturn(List.of(buildDTO(1L, "Spotlight")));

                mockMvc.perform(get("/books/spotlight/all")
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));
        }

        // ─── GET /books/latest ────────────────────────────────────────────────────

        @Test
        void getLatestBooks_withoutReadingLevel_callsDefaultMethod() throws Exception {
                when(bookService.getLatestBooks(UserRoles.STUDENT, null))
                                .thenReturn(List.of(buildDTO(1L, "Latest")));

                mockMvc.perform(get("/books/latest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(bookService, never()).getLatestBooks(any(), any(), any());
        }

        @Test
        void getLatestBooks_withReadingLevel_callsFilteredMethod() throws Exception {
                when(bookService.getLatestBooks(UserRoles.STUDENT, null, "B"))
                                .thenReturn(List.of(buildDTO(2L, "Niveau B")));

                mockMvc.perform(get("/books/latest")
                                .param("readingLevel", "B"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Niveau B"));

                verify(bookService, never()).getLatestBooks(UserRoles.STUDENT, null);
        }

        // ─── PATCH /books/{id}/spotlight — @PreAuthorize ──────────────────────────

        @Test
        void updateSpotlight_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(patch("/books/1/spotlight")
                                .param("value", "true")
                                .with(oauth2Login()))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void updateSpotlight_asLibrarian_returns204() throws Exception {
                doNothing().when(bookService).updateSpotlight(1L, true);

                mockMvc.perform(patch("/books/1/spotlight")
                                .param("value", "true")
                                .with(oauth2Login()
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isNoContent());

                verify(bookService).updateSpotlight(1L, true);
        }

        @Test
        void updateSpotlight_bookNotFound_returns404() throws Exception {
                doThrow(new BookNotFoundException(99L)).when(bookService).updateSpotlight(99L, false);

                mockMvc.perform(patch("/books/99/spotlight")
                                .param("value", "false")
                                .with(oauth2Login()
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists());
        }

        // ─── POST /books/add/{isbn} — @PreAuthorize ───────────────────────────────

        @Test
        void addBookByIsbn_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(post("/books/add/9780132350884")
                                .with(oauth2Login()))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void addBookByIsbn_asLibrarian_returns201() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.addBookByIsbn("9780132350884", "uid-lib", "Campus Zuid", null))
                                .thenReturn(buildDTO(1L, "Clean Code"));

                mockMvc.perform(post("/books/add/9780132350884")
                                .param("campus", "Campus Zuid")
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Clean Code"));
        }

        @Test
        void addBookByIsbn_unknownIsbn_returns400() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.addBookByIsbn(eq("0000000000000"), eq("uid-lib"), any(), any()))
                                .thenThrow(new IllegalArgumentException("Geen boek voor ISBN: 0000000000000"));

                mockMvc.perform(post("/books/add/0000000000000")
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Geen boek voor ISBN: 0000000000000"));
        }

        @Test
        void addBookByIsbn_unexpectedError_returns500() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.addBookByIsbn(any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Google API down"));

                mockMvc.perform(post("/books/add/9780132350884")
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message").value("Er is een onverwachte fout opgetreden"));
        }

        // ─── GET /books/search/{isbn} ─────────────────────────────────────────────

        @Test
        void searchBookByIsbn_found_returnsOk() throws Exception {
                when(bookService.searchBookByIsbn("9780132350884")).thenReturn(buildDTO(1L, "Clean Code"));

                mockMvc.perform(get("/books/search/9780132350884"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Clean Code"));
        }

        @Test
        void searchBookByIsbn_unknownIsbn_returns400() throws Exception {
                when(bookService.searchBookByIsbn("0000000000000"))
                                .thenThrow(new IllegalArgumentException("Geen boek voor ISBN: 0000000000000"));

                mockMvc.perform(get("/books/search/0000000000000"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Geen boek voor ISBN: 0000000000000"));
        }

        // ─── PATCH /books/{id} — @PreAuthorize ───────────────────────────────────

        @Test
        void updateBook_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(patch("/books/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildDTOJson(1L, "Test"))
                                .with(oauth2Login()))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void updateBook_asLibrarian_returnsOk() throws Exception {
                BookDTO dto = buildDTO(1L, "Updated Title");
                when(bookService.updateBook(eq(1L), any())).thenReturn(dto);

                mockMvc.perform(patch("/books/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildDTOJson(1L, "Updated Title"))
                                .with(oauth2Login()
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        void updateBook_bookNotFound_returns404() throws Exception {
                when(bookService.updateBook(eq(99L), any())).thenThrow(new BookNotFoundException(99L));

                mockMvc.perform(patch("/books/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildDTOJson(99L, "Some Title"))
                                .with(oauth2Login()
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void updateBook_illegalArgument_returns400() throws Exception {
                when(bookService.updateBook(eq(1L), any()))
                                .thenThrow(new IllegalArgumentException("Titel mag niet leeg zijn"));

                mockMvc.perform(patch("/books/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildDTOJson(1L, ""))
                                .with(oauth2Login()
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Titel mag niet leeg zijn"));
        }

        // ─── POST /books/add — @PreAuthorize ─────────────────────────────────────

        @Test
        void addManualBook_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(post("/books/add")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildCreateRequestJson("New Book"))
                                .with(oauth2Login()))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void addManualBook_asLibrarian_returns201() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.addManualBook(any(), eq("uid-lib"))).thenReturn(buildDTO(1L, "New Book"));

                mockMvc.perform(post("/books/add")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildCreateRequestJson("New Book"))
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("New Book"));
        }

        @Test
        void addManualBook_illegalArgument_returns400() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.addManualBook(any(), eq("uid-lib")))
                                .thenThrow(new IllegalArgumentException("Titel is verplicht"));

                mockMvc.perform(post("/books/add")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildCreateRequestJson(""))
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Titel is verplicht"));
        }

        // ─── POST /books/import — controller heeft eigen try-catch ────────────────

        @Test
        void importBooks_withoutLibrarianRole_returns403() throws Exception {
                mockMvc.perform(multipart("/books/import")
                                .file(dummyExcelFile())
                                .with(oauth2Login()))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(bookService);
        }

        @Test
        void importBooks_validFile_returnsOk() throws Exception {
                stubAsLibrarian("uid-lib");
                BulkImportResponseDTO response = new BulkImportResponseDTO(3, 2, 1,
                                List.of(new ImportMismatchDTO(4, "9780132350884", "Wrong", "Clean Code",
                                                "De titel komt niet overeen (Clean Code)")));

                when(bookService.importBooksFromExcel(any(), eq("uid-lib"), eq("Campus Zuid"), eq(true),
                                eq(List.of(2))))
                                .thenReturn(response);

                mockMvc.perform(multipart("/books/import")
                                .file(dummyExcelFile())
                                .param("campus", "Campus Zuid")
                                .param("confirmDuplicates", "true")
                                .param("confirmedDuplicateRows", "2")
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isOk());
        }

        @Test
        void importBooks_emptyFile_returns400() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.importBooksFromExcel(any(), any(), any(), anyBoolean(), any()))
                                .thenThrow(new IllegalArgumentException("Upload een excel file die niet leeg is"));

                mockMvc.perform(multipart("/books/import")
                                .file(dummyExcelFile())
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Upload een excel file die niet leeg is"));
        }

        @Test
        void importBooks_unexpectedError_returns500() throws Exception {
                stubAsLibrarian("uid-lib");
                when(bookService.importBooksFromExcel(any(), any(), any(), anyBoolean(), any()))
                                .thenThrow(new RuntimeException("DB down"));

                mockMvc.perform(multipart("/books/import")
                                .file(dummyExcelFile())
                                .with(oauth2Login()
                                                .attributes(a -> a.put("userID", "uid-lib"))
                                                .authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTHEEKBEHEERDER"))))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.message")
                                                .value("Er is een fout opgetreden bij het importeren van het Excelbestand."));
        }

        // ─── GET /books/{id}/snowball ─────────────────────────────────────────────

        @Test
        void getSnowball_returnsRelatedBooks() throws Exception {
                when(bookService.getSnowballSections(1L, UserRoles.STUDENT, null))
                                .thenReturn(List.of(new SnowballSectionDTO("AUTHOR", "Auteur X",
                                                List.of(buildDTO(2L, "Boek B")))));

                mockMvc.perform(get("/books/1/snowball"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].type").value("AUTHOR"));
        }

        @Test
        void getSnowball_bookNotFound_returns404() throws Exception {
                when(bookService.getSnowballSections(eq(99L), any(), any()))
                                .thenThrow(new BookNotFoundException(99L));

                mockMvc.perform(get("/books/99/snowball"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists());
        }

        // ─── private helpers ──────────────────────────────────────────────────────

        private MockMultipartFile dummyExcelFile() {
                return new MockMultipartFile("file", "books.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes());
        }

        private String buildDTOJson(Long id, String title) {
                return """
                                {"id":%d,"title":"%s","authors":["Author"],"publisher":"Publisher",
                                 "description":"Description","pageCount":100,"categories":["Category"],
                                 "thumbnail":"thumbnail","language":"en","averageRating":4.0,
                                 "isbn":"9781234567890","publishedYear":2023,"spotlight":false,
                                 "labels":null,"readingLevel":"A","totalCopies":1,"availableCopies":1,
                                 "readingLevelDescription":"Eerste graad","previewLink":null,"inventories":null}
                                """.formatted(id, title);
        }

        private String buildCreateRequestJson(String title) {
                return """
                                {"title":"%s","authors":["Author"],"publisher":"Publisher",
                                 "description":"Description","pageCount":100,"categories":["Category"],
                                 "thumbnail":"thumbnail","language":"nl","rating":4.0,
                                 "publishedYear":2023,"spotlight":false,"didacticTag":false,
                                 "labels":null,"readingLevel":"A","totalCopies":1,"availableCopies":1,
                                 "ageRange":"Eerste graad","inventories":null}
                                """.formatted(title);
        }

}
