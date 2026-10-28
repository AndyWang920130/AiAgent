package com.example.myapp.web.rest.vm;

import com.example.myapp.contants.enumeration.Gender;
import com.example.myapp.contants.enumeration.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fields an administrator supplies when creating an account directly, bypassing the
 * email-verification step that self-registration requires.
 *
 * <p>The password arrives in plain text and is encoded before it is stored — the account
 * can be used immediately, unlike one written through the old generated CRUD endpoint.
 */
public record AdminCreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Login may only contain letters, digits, dot, underscore and dash")
        String login,

        @NotBlank @Size(max = 512) String realName,

        @NotBlank @Email @Size(max = 512) String email,

        @NotBlank @Size(min = 6, max = 100) String password,

        /** A role code from the Role Config catalog; must name an enabled role. */
        @NotBlank @Size(max = 50) String role,

        @Size(max = 512) String nickName,
        Gender gender,
        UserType userType,
        @Size(max = 32) String phoneNumber,
        @Size(max = 200) String description
) {}
