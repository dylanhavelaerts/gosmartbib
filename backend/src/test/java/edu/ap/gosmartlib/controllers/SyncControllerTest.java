package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.repositories.UserRepository;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock private OneRosterSyncService syncService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SyncController syncController;

    @Test
    void givenRequest_whenSyncAll_thenDelegatesToService() {
        SyncSummaryDTO expected = new SyncSummaryDTO(3, 1, List.of());
        when(syncService.syncAll()).thenReturn(expected);

        SyncSummaryDTO result = syncController.syncAll();

        assertEquals(expected, result);
        verify(syncService).syncAll();
        verifyNoMoreInteractions(syncService);
    }
}
