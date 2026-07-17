package com.example.myapp.web.rest;

import com.example.myapp.service.BlogCommentService;
import com.example.myapp.service.dto.BlogCommentCreateDTO;
import com.example.myapp.service.dto.BlogCommentDTO;
import com.example.myapp.utils.PageUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
public class BlogCommentResource {
    private final BlogCommentService blogCommentService;

    public BlogCommentResource(BlogCommentService blogCommentService) {
        this.blogCommentService = blogCommentService;
    }

    @GetMapping("/blogs/{blogId}/comments")
    public ResponseEntity<List<BlogCommentDTO>> list(
        @PathVariable Long blogId,
        @PageableDefault(sort = "createdDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return blogCommentService.list(blogId, pageable)
            .map(page -> {
                HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
                return ResponseEntity.ok().headers(headers).body(page.getContent());
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/blogs/{blogId}/comments")
    public ResponseEntity<BlogCommentDTO> create(@PathVariable Long blogId, @Valid @RequestBody BlogCommentCreateDTO request) {
        return blogCommentService.create(blogId, request.content())
            .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        blogCommentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
