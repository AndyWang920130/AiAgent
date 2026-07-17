package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

public record BlogCommentDTO(
    Long id, Long blogId, String username, String content, Instant createdDate, boolean canDelete
) implements Serializable {
}
