package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * A notification as delivered to the client. {@code type} is lowercase to match the frontend's
 * icon/colour keys (info/success/warning/error). {@code link} is an optional in-app path.
 */
public record NotificationDTO(
    Long id,
    String type,
    String title,
    String content,
    String link,
    boolean read,
    Instant createdDate
) implements Serializable {
}
