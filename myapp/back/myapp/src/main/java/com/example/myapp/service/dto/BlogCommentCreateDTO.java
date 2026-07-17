package com.example.myapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogCommentCreateDTO(@NotBlank @Size(max = 2000) String content) {
}
