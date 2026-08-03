package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Public view of a user's profile, as shown when another user opens their page.
 * Contains only non-sensitive fields (no email, phone, password) plus aggregate
 * activity stats and the current viewer's follow relationship to this user.
 */
public record PublicUserProfileDTO(
    String login,
    String name,
    String bio,
    String avatar,
    String gender,
    Instant joinDate,
    long postCount,
    long likesReceived,
    long achievementPoints,
    long followerCount,
    long followingCount,
    boolean following
) implements Serializable {
}
