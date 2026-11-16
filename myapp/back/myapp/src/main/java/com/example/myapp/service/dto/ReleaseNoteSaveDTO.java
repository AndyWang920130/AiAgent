package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * What the editor posts, for both create and update. The lengths match the columns on
 * {@link com.example.myapp.domain.ReleaseNote} so an over-long value is a 400 rather than a
 * MySQL data-length error.
 *
 * <p>{@code content} carries no {@code @Size}: the column is {@code longtext} and a release
 * note has no useful limit. It is {@code @NotBlank}, but blankness is also re-checked in the
 * service — the editor sends {@code <p></p>} for an empty body, which is not blank to this
 * annotation.
 *
 * <p>{@code status} is optional and defaults to {@code DRAFT}, so a note cannot be made public
 * by omission.
 */
public record ReleaseNoteSaveDTO(
    @NotBlank @Size(max = 50)
    String version,

    @Size(max = 200)
    String title,

    @NotBlank
    String content,

    @NotNull
    Instant releasedDate,

    String status
) implements Serializable {
}
