package edu.ap.gosmartlib.dto.userdirectory;

import java.util.List;

public record ResolveDisplayNamesRequest(
        List<String> uids,
        Long schoolId) {
}