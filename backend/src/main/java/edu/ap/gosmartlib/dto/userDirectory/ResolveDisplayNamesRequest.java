package edu.ap.gosmartlib.dto.userDirectory;

import java.util.List;

public record ResolveDisplayNamesRequest(
        List<String> uids) {
}