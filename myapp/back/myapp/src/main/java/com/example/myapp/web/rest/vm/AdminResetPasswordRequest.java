package com.example.myapp.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An administrator setting a new password for an account. The current password is not
 * required — that check belongs to {@link ChangePasswordRequest}, where the user is
 * changing their own.
 */
public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {}
