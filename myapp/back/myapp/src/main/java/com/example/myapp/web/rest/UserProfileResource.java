package com.example.myapp.web.rest;

import com.example.myapp.service.UserProfileService;
import com.example.myapp.service.dto.BlogDTO;
import com.example.myapp.service.dto.FollowStatusDTO;
import com.example.myapp.service.dto.FollowUserDTO;
import com.example.myapp.service.dto.PublicUserProfileDTO;
import com.example.myapp.service.dto.UserSearchDTO;
import com.example.myapp.utils.PageUtils;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller exposing a user's public profile and follow / unfollow actions.
 * All endpoints require authentication (see {@code SecurityConfig#anyRequest().authenticated()}).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileResource {

    private final UserProfileService userProfileService;

    public UserProfileResource(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /** Search users for the header search box. Exact path precedence over {@code /{login}}. */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDTO>> search(@RequestParam("q") String q) {
        return ResponseEntity.ok(userProfileService.searchUsers(q));
    }

    @GetMapping("/{login}")
    public ResponseEntity<PublicUserProfileDTO> getProfile(@PathVariable String login) {
        return userProfileService.getPublicProfile(login)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{login}/blogs")
    public ResponseEntity<List<BlogDTO>> getBlogs(
        @PathVariable String login,
        @PageableDefault(size = 100, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BlogDTO> page = userProfileService.getPublicBlogs(login, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{login}/follow-status")
    public ResponseEntity<FollowStatusDTO> followStatus(@PathVariable String login) {
        return ResponseEntity.ok(userProfileService.getFollowStatus(login));
    }

    @GetMapping("/{login}/followers")
    public ResponseEntity<List<FollowUserDTO>> followers(
        @PathVariable String login,
        @PageableDefault(size = 100, sort = "followedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FollowUserDTO> page = userProfileService.getFollowers(login, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{login}/following")
    public ResponseEntity<List<FollowUserDTO>> following(
        @PathVariable String login,
        @PageableDefault(size = 100, sort = "followedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FollowUserDTO> page = userProfileService.getFollowing(login, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/{login}/follow")
    public ResponseEntity<FollowStatusDTO> follow(@PathVariable String login) {
        return ResponseEntity.ok(userProfileService.follow(login));
    }

    @DeleteMapping("/{login}/follow")
    public ResponseEntity<FollowStatusDTO> unfollow(@PathVariable String login) {
        return ResponseEntity.ok(userProfileService.unfollow(login));
    }
}
