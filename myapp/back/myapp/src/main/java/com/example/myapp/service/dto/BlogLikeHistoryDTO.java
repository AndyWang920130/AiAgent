package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

public record BlogLikeHistoryDTO(
    Long id, String username, Long blogId, String blogTitle,
    String blogAuthor, Long blogLikes, Instant likedDate
) implements Serializable {
}
