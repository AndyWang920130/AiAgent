package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * A submission as delivered to the client. {@code username} and {@code name} identify the
 * submitter for the administrator's list; the submitter's own list shows their own rows, so
 * both are filled in either way rather than switched on the caller's role.
 *
 * <p>{@code reply} is null until an administrator answers.
 */
public record FeedbackDTO(
    Long id,
    String username,
    String name,
    String category,
    String title,
    String content,
    String contact,
    String status,
    String reply,
    String repliedBy,
    Instant repliedDate,
    Instant createdDate
) implements Serializable {
}
