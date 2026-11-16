package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * A publish or un-publish on its own. Exists so the one-click Publish on the list does not
 * have to send the whole HTML body back to change one column — which would also mean a stale
 * editor tab could overwrite an edit it never saw.
 */
public record ReleaseNoteStatusDTO(
    @NotBlank
    String status
) implements Serializable {
}
