package com.example.myapp.web.rest.vm;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Disable or re-enable an account. Disabling is the soft delete the {@code deleted} and
 * {@code delete_reason} columns were built for: the row stays, so everything that
 * references the login by value (blogs, comments, stock watches) keeps resolving.
 *
 * @param deleted true to disable the account, false to re-enable it.
 * @param reason  why it was disabled; ignored when re-enabling.
 */
public record AdminSetUserStatusRequest(
        @NotNull Boolean deleted,
        @Size(max = 100) String reason
) {}
