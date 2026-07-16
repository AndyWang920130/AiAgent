package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

public record BlogLikeStatusDTO(Long blogId, Long totalLikes, boolean liked, Instant likedDate) implements Serializable {
}
