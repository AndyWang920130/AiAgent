package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Records that one user follows another. A user can follow another user only once. */
@Entity
@Table(
    name = "twsny_user_follow",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_twsny_user_follow_follower_following",
        columnNames = {"follower_username", "following_username"}
    ),
    indexes = {
        @Index(name = "idx_twsny_user_follow_follower", columnList = "follower_username"),
        @Index(name = "idx_twsny_user_follow_following", columnList = "following_username"),
        @Index(name = "idx_twsny_user_follow_followed_date", columnList = "followed_date")
    }
)
public class UserFollow extends AbstractAuditingEntity {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @Size(max = 100) @Column(name = "follower_username", nullable = false, length = 100)
    private String followerUsername;
    @NotNull @Size(max = 100) @Column(name = "following_username", nullable = false, length = 100)
    private String followingUsername;
    @NotNull @Column(name = "followed_date", nullable = false)
    private Instant followedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFollowerUsername() { return followerUsername; }
    public void setFollowerUsername(String followerUsername) { this.followerUsername = followerUsername; }
    public String getFollowingUsername() { return followingUsername; }
    public void setFollowingUsername(String followingUsername) { this.followingUsername = followingUsername; }
    public Instant getFollowedDate() { return followedDate; }
    public void setFollowedDate(Instant followedDate) { this.followedDate = followedDate; }
    @Override public boolean equals(Object o) {
        return this == o || o instanceof UserFollow other && getId() != null && getId().equals(other.getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
