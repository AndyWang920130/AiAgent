package com.example.myapp.web.rest;

import com.example.myapp.service.BlogService;
import com.example.myapp.service.BlogViewHistoryService;
import com.example.myapp.service.dto.BlogDTO;
import com.example.myapp.service.dto.BlogViewHistoryDTO;
import com.example.myapp.utils.PageUtils;
import com.example.myapp.web.rest.vm.BlogStats;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing Blog.
 */
@RestController
@RequestMapping("/api/v1")
public class BlogResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlogResource.class);

    private static final String ENTITY_NAME = "blog";

    private final BlogService blogService;

    private final BlogViewHistoryService blogViewHistoryService;

    public BlogResource(BlogService blogService, BlogViewHistoryService blogViewHistoryService) {
        this.blogService = blogService;
        this.blogViewHistoryService = blogViewHistoryService;
    }

    /**
     * POST /blogs : Create a new blog.
     */
    @PostMapping("/blogs")
    public ResponseEntity<BlogDTO> createBlog(@Valid @RequestBody BlogDTO blogDTO) throws URISyntaxException {
        LOG.debug("REST request to save Blog : {}", blogDTO);
        if (blogDTO.getId() != null) {
            throw new BadRequestAlertException("A new blog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        BlogDTO result = blogService.save(blogDTO);
        return ResponseEntity
            .created(new URI("/api/v1/blogs/" + result.getId()))
            .body(result);
    }

    /**
     * PUT /blogs/{id} : Updates an existing blog.
     */
    @PutMapping("/blogs/{id}")
    public ResponseEntity<BlogDTO> updateBlog(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody BlogDTO blogDTO
    ) {
        LOG.debug("REST request to update Blog : {}, {}", id, blogDTO);
        if (blogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, blogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        BlogDTO result = blogService.update(blogDTO);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /blogs/{id} : Partial updates given fields of an existing blog.
     */
    @PatchMapping(value = "/blogs/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<BlogDTO> partialUpdateBlog(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody BlogDTO blogDTO
    ) {
        LOG.debug("REST request to partial update Blog partially : {}, {}", id, blogDTO);
        if (blogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, blogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        Optional<BlogDTO> result = blogService.partialUpdate(blogDTO);
        return result
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /blogs : Get all the blogs.
     */
    @GetMapping("/blogs")
    public ResponseEntity<List<BlogDTO>> getAllBlogs(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        LOG.debug("REST request to get a page of public Blogs");
        Page<BlogDTO> page = blogService.findAll(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /blogs/stats : Get site-wide aggregate stats over public blogs.
     */
    @GetMapping("/blogs/stats")
    public ResponseEntity<BlogStats> getBlogStats() {
        LOG.debug("REST request to get public Blog stats");
        return ResponseEntity.ok(blogService.getStats());
    }

    /**
     * GET /blogs/following : Get the following feed — public blogs authored by users the
     * current user follows, newest first.
     */
    @GetMapping("/blogs/following")
    public ResponseEntity<List<BlogDTO>> getFollowingBlogs(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        LOG.debug("REST request to get a page of the current user's following feed");
        Page<BlogDTO> page = blogService.findFollowing(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /blogs/my : Get current user's blogs.
     */
    @GetMapping("/blogs/my")
    public ResponseEntity<List<BlogDTO>> getMyBlogs(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        LOG.debug("REST request to get a page of current user's Blogs");
        Page<BlogDTO> page = blogService.findMine(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /blogs/{id} : Get the blog by id.
     */
    @GetMapping("/blogs/{id}")
    public ResponseEntity<BlogDTO> getBlog(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Blog : {}", id);
        Optional<BlogDTO> blogDTO = blogService.findOne(id);
        return blogDTO
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /blogs/{id}/view : Increment view count.
     */
    @PostMapping("/blogs/{id}/view")
    public ResponseEntity<BlogViewHistoryDTO> incrementView(@PathVariable("id") Long id) {
        LOG.debug("REST request to increment view count for Blog : {}", id);
        Optional<BlogViewHistoryDTO> result = blogService.incrementViewCount(id);
        return result
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /blogs/view-history/my : Get current user's blog view history.
     */
    @GetMapping("/blogs/view-history/my")
    public ResponseEntity<List<BlogViewHistoryDTO>> getMyViewHistory(
        @PageableDefault(sort = {"lastViewedDate"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LOG.debug("REST request to get current user's Blog view history");
        Page<BlogViewHistoryDTO> page = blogViewHistoryService.findMine(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * DELETE /blogs/{id} : Delete the blog by id.
     */
    @DeleteMapping("/blogs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBlog(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Blog : {}", id);
        blogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
