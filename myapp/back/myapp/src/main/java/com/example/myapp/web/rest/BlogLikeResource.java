package com.example.myapp.web.rest;

import com.example.myapp.service.BlogLikeService;
import com.example.myapp.service.dto.BlogLikeHistoryDTO;
import com.example.myapp.service.dto.BlogLikeStatusDTO;
import com.example.myapp.utils.PageUtils;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogLikeResource {
    private final BlogLikeService blogLikeService;

    public BlogLikeResource(BlogLikeService blogLikeService) {
        this.blogLikeService = blogLikeService;
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<BlogLikeStatusDTO> like(@PathVariable Long id) {
        return blogLikeService.like(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/like-status")
    public ResponseEntity<BlogLikeStatusDTO> status(@PathVariable Long id) {
        return blogLikeService.getStatus(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/like-history/my")
    public ResponseEntity<List<BlogLikeHistoryDTO>> history(
        @PageableDefault(sort = "likedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BlogLikeHistoryDTO> page = blogLikeService.findMine(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/likes-received/my")
    public Map<String, Long> likesReceived() {
        return Map.of("totalLikes", blogLikeService.getLikesReceived());
    }
}
