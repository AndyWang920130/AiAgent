package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * What the submit form posts. The lengths match the column lengths on
 * {@link com.example.myapp.domain.Feedback}, so an over-long body is refused as a 400 rather
 * than truncated or raised as a data-length error by MySQL.
 *
 * <p>{@code category} is optional and falls back to {@code OTHER}; an unrecognised value is
 * refused by the service.
 */
public record FeedbackCreateDTO(
    String category,

    @NotBlank @Size(max = 200)
    String title,

    @NotBlank @Size(max = 2000)
    String content,

    @Size(max = 200)
    String contact
) implements Serializable {
}
