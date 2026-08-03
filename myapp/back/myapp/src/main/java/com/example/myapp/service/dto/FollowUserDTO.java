package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * A single entry in a follower / following list: the other user's login, display name,
 * avatar, and the date the follow relationship was created.
 */
public record FollowUserDTO(String login, String name, String avatar, Instant followedDate) implements Serializable {
}
