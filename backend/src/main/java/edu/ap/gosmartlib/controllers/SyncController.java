package edu.ap.gosmartlib.controllers;

import edu.ap.gosmartlib.dto.sync.SyncSummaryDTO;
import edu.ap.gosmartlib.services.oneroster.OneRosterSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final OneRosterSyncService syncService;

    @PostMapping
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public SyncSummaryDTO syncAll() {
        return syncService.syncAll();
    }

}
