package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * A release note as delivered to the client.
 *
 * <p>{@code author} is the login that wrote it and {@code authorName} that account's current
 * display name, resolved at read time. {@code status} reaches a non-administrator as
 * {@code PUBLISHED} only — the public endpoint lists nothing else — but it is carried either
 * way so the administrator's page can mark drafts without a second shape of this record.
 */
public record ReleaseNoteDTO(
    Long id,
    String version,
    String title,
    String content,
    Instant releasedDate,
    String status,
    String author,
    String authorName,
    Instant lastModifiedDate
) implements Serializable {
}
