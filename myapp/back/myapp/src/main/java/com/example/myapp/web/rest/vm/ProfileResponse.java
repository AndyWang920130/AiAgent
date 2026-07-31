package com.example.myapp.web.rest.vm;

import java.time.Instant;

/**
 * The current user's editable profile, as returned by GET/PUT /api/v1/auth/profile.
 * {@code username} and {@code joinDate} are read-only (shown but never accepted on update).
 */
public record ProfileResponse(String username, String name, String email, String bio, Instant joinDate) {}
