package com.example.myapp.web.rest;

import com.example.myapp.contants.enumeration.BlogConfigType;
import com.example.myapp.service.BlogConfigService;
import com.example.myapp.service.dto.BlogConfigDTO;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class BlogConfigResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlogConfigResource.class);

    private static final String ENTITY_NAME = "blogConfig";

    private final BlogConfigService blogConfigService;

    public BlogConfigResource(BlogConfigService blogConfigService) {
        this.blogConfigService = blogConfigService;
    }

    @PostMapping("/blog-configs")
    public ResponseEntity<BlogConfigDTO> createBlogConfig(@Valid @RequestBody BlogConfigDTO blogConfigDTO) throws URISyntaxException {
        LOG.debug("REST request to save BlogConfig : {}", blogConfigDTO);
        if (blogConfigDTO.getId() != null) {
            throw new BadRequestAlertException("A new blog config cannot already have an ID", ENTITY_NAME, "idexists");
        }
        BlogConfigDTO result = blogConfigService.save(blogConfigDTO);
        return ResponseEntity.created(new URI("/api/v1/blog-configs/" + result.getId())).body(result);
    }

    @PutMapping("/blog-configs/{id}")
    public ResponseEntity<BlogConfigDTO> updateBlogConfig(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody BlogConfigDTO blogConfigDTO
    ) {
        LOG.debug("REST request to update BlogConfig : {}, {}", id, blogConfigDTO);
        if (blogConfigDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, blogConfigDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        return ResponseEntity.ok(blogConfigService.update(blogConfigDTO));
    }

    @GetMapping("/blog-configs")
    public ResponseEntity<List<BlogConfigDTO>> getAllBlogConfigs(@RequestParam(required = false) BlogConfigType type) {
        LOG.debug("REST request to get BlogConfig list by type : {}", type);
        return ResponseEntity.ok(blogConfigService.findAll(Optional.ofNullable(type)));
    }

    @GetMapping("/blog-configs/{id}")
    public ResponseEntity<BlogConfigDTO> getBlogConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to get BlogConfig : {}", id);
        return blogConfigService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/blog-configs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBlogConfig(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete BlogConfig : {}", id);
        blogConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
