package com.example.myapp.service;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.domain.User;
import com.example.myapp.domain.UserFollow;
import com.example.myapp.repository.BlogLockRepository;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.repository.UserFollowRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.BlogDTO;
import com.example.myapp.service.dto.FollowStatusDTO;
import com.example.myapp.service.dto.FollowUserDTO;
import com.example.myapp.service.dto.PublicUserProfileDTO;
import com.example.myapp.service.dto.UserSearchDTO;
import com.example.myapp.service.mapper.BlogMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles a user's public profile and manages follow / unfollow relationships.
 * The follow flow mirrors {@link BlogLikeService}: idempotent create, count via repository.
 */
@Service
@Transactional
public class UserProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private static final String ENTITY_NAME = "userFollow";

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final BlogRepository blogRepository;
    private final BlogLockRepository blogLockRepository;
    private final BlogMapper blogMapper;
    private final AchievementService achievementService;

    public UserProfileService(
        UserRepository userRepository,
        UserFollowRepository userFollowRepository,
        BlogRepository blogRepository,
        BlogLockRepository blogLockRepository,
        BlogMapper blogMapper,
        AchievementService achievementService
    ) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.blogRepository = blogRepository;
        this.blogLockRepository = blogLockRepository;
        this.blogMapper = blogMapper;
        this.achievementService = achievementService;
    }

    /**
     * Assemble the public profile for {@code login}, or empty if no such user exists.
     */
    @Transactional(readOnly = true)
    public Optional<PublicUserProfileDTO> getPublicProfile(String login) {
        LOG.debug("Request to get public profile of {}", login);
        return userRepository.findOneByLogin(login).map(user -> {
            String current = SecurityUtil.getCurrentUsername();
            long likes = Optional.ofNullable(blogLockRepository.sumLikesByAuthor(login)).orElse(0L);
            long postCount = blogRepository.countByAuthorAndVisibility(login, BlogVisibility.PUBLIC);
            long achievementPoints = achievementService.getSummary(login).total();
            return new PublicUserProfileDTO(
                user.getLogin(),
                displayName(user),
                user.getDescription(),
                user.getAvatar(),
                user.getGender() == null ? null : user.getGender().name(),
                user.getCreatedDate(),
                postCount,
                likes,
                achievementPoints,
                userFollowRepository.countByFollowingUsername(login),
                userFollowRepository.countByFollowerUsername(login),
                userFollowRepository.existsByFollowerUsernameAndFollowingUsername(current, login)
            );
        });
    }

    /**
     * Search users by login / real name / nickname (case-insensitive, partial), for the header
     * search box. Excludes the current user and is capped at 10 results.
     */
    @Transactional(readOnly = true)
    public List<UserSearchDTO> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String current = SecurityUtil.getCurrentUsername();
        return userRepository.search(query.trim(), PageRequest.of(0, 10)).stream()
            .filter(user -> !user.getLogin().equals(current))
            .map(user -> new UserSearchDTO(user.getLogin(), displayName(user), user.getAvatar()))
            .toList();
    }

    /**
     * The public (published & PUBLIC) blogs authored by {@code login}.
     */
    @Transactional(readOnly = true)
    public Page<BlogDTO> getPublicBlogs(String login, Pageable pageable) {
        return blogRepository.findByAuthorAndVisibility(login, BlogVisibility.PUBLIC, pageable).map(blogMapper::toDto);
    }

    /**
     * Follow {@code targetLogin} on behalf of the current user. Idempotent — following an
     * already-followed user is a no-op. Rejects following oneself (400) and unknown users (404).
     */
    public FollowStatusDTO follow(String targetLogin) {
        String current = SecurityUtil.getCurrentUsername();
        if (current.equals(targetLogin)) {
            throw new BadRequestAlertException("You cannot follow yourself", ENTITY_NAME, "followself");
        }
        requireUserExists(targetLogin);
        if (!userFollowRepository.existsByFollowerUsernameAndFollowingUsername(current, targetLogin)) {
            UserFollow follow = new UserFollow();
            follow.setFollowerUsername(current);
            follow.setFollowingUsername(targetLogin);
            follow.setFollowedDate(Instant.now());
            follow.setCreatedBy(current);
            follow.setLastModifiedBy(current);
            userFollowRepository.save(follow);
        }
        return status(current, targetLogin);
    }

    /**
     * Unfollow {@code targetLogin} on behalf of the current user. No-op if not currently followed.
     */
    public FollowStatusDTO unfollow(String targetLogin) {
        String current = SecurityUtil.getCurrentUsername();
        requireUserExists(targetLogin);
        userFollowRepository.deleteByFollowerUsernameAndFollowingUsername(current, targetLogin);
        return status(current, targetLogin);
    }

    @Transactional(readOnly = true)
    public FollowStatusDTO getFollowStatus(String targetLogin) {
        requireUserExists(targetLogin);
        return status(SecurityUtil.getCurrentUsername(), targetLogin);
    }

    /**
     * The users who follow {@code login} (that user's followers), newest first.
     */
    @Transactional(readOnly = true)
    public Page<FollowUserDTO> getFollowers(String login, Pageable pageable) {
        requireUserExists(login);
        return mapFollowUsers(
            userFollowRepository.findByFollowingUsernameOrderByFollowedDateDesc(login, pageable),
            UserFollow::getFollowerUsername
        );
    }

    /**
     * The users that {@code login} follows, newest first.
     */
    @Transactional(readOnly = true)
    public Page<FollowUserDTO> getFollowing(String login, Pageable pageable) {
        requireUserExists(login);
        return mapFollowUsers(
            userFollowRepository.findByFollowerUsernameOrderByFollowedDateDesc(login, pageable),
            UserFollow::getFollowingUsername
        );
    }

    /**
     * Map a page of follow rows to {@link FollowUserDTO}, resolving each relevant login to its
     * display name / avatar with a single batched user lookup (no N+1).
     */
    private Page<FollowUserDTO> mapFollowUsers(Page<UserFollow> page, Function<UserFollow, String> loginExtractor) {
        List<String> logins = page.getContent().stream().map(loginExtractor).toList();
        Map<String, User> byLogin = logins.isEmpty()
            ? Map.of()
            : userRepository.findByLoginIn(logins).stream().collect(Collectors.toMap(User::getLogin, u -> u, (a, b) -> a));
        return page.map(follow -> {
            String otherLogin = loginExtractor.apply(follow);
            User user = byLogin.get(otherLogin);
            return new FollowUserDTO(
                otherLogin,
                user == null ? otherLogin : displayName(user),
                user == null ? null : user.getAvatar(),
                follow.getFollowedDate()
            );
        });
    }

    private FollowStatusDTO status(String current, String targetLogin) {
        return new FollowStatusDTO(
            targetLogin,
            userFollowRepository.countByFollowingUsername(targetLogin),
            userFollowRepository.countByFollowerUsername(targetLogin),
            userFollowRepository.existsByFollowerUsernameAndFollowingUsername(current, targetLogin)
        );
    }

    private void requireUserExists(String login) {
        if (!userRepository.existsByLogin(login)) {
            throw new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound");
        }
    }

    private String displayName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        return user.getLogin();
    }
}
