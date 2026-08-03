package com.example.myapp.repository;

import com.example.myapp.domain.UserFollow;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    /** People who follow the given user (the user's followers), newest first. */
    Page<UserFollow> findByFollowingUsernameOrderByFollowedDateDesc(String followingUsername, Pageable pageable);

    /** People the given user follows, newest first. */
    Page<UserFollow> findByFollowerUsernameOrderByFollowedDateDesc(String followerUsername, Pageable pageable);

    boolean existsByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    /** Number of users following the given user. */
    long countByFollowingUsername(String followingUsername);

    /** Number of users the given user follows. */
    long countByFollowerUsername(String followerUsername);

    void deleteByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);
}
