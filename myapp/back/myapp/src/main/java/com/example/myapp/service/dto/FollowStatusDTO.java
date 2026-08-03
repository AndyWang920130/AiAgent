package com.example.myapp.service.dto;

import java.io.Serializable;

/**
 * The follow relationship between the current user and a target user, plus the
 * target's follower/following counts. Returned by the follow / unfollow / status endpoints.
 */
public record FollowStatusDTO(String username, long followerCount, long followingCount, boolean following)
    implements Serializable {
}
