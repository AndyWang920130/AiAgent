package com.example.myapp.web.rest.vm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields the current user may change on their own profile. Only the columns that have real
 * backing storage are here: name -> realName, email, bio -> description. The account login
 * (username) is immutable, so it is not accepted.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 512) String name,
        @NotBlank @Email @Size(max = 512) String email,
        @Size(max = 200) String bio
) {}
