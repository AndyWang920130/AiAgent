package com.example.myapp.web.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String name,
        String email,
        @NotBlank @Size(min = 6) String password
) {}
