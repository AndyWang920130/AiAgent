package com.example.myapp.web.rest.vm;

import com.example.myapp.contants.enumeration.Gender;
import com.example.myapp.contants.enumeration.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields an administrator may change on another account.
 *
 * <p>{@code login} is absent because it is the account's identity and is referenced by
 * value elsewhere (blog authors, stock watches, notifications), so renaming it would
 * orphan that data. {@code password} is absent too — it goes through the dedicated
 * reset endpoint so it is always encoded on the way in.
 */
public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 512) String realName,

        @NotBlank @Email @Size(max = 512) String email,

        /** A role code from the Role Config catalog; must name an enabled role. */
        @NotBlank @Size(max = 50) String role,

        @Size(max = 512) String nickName,
        Gender gender,
        UserType userType,
        @Size(max = 32) String phoneNumber,
        @Size(max = 200) String description
) {}
