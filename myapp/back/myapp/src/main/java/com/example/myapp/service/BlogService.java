package com.example.myapp.service;

import com.example.myapp.contants.enumeration.AchievementType;
import com.example.myapp.contants.enumeration.BlogStatus;
import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.domain.Blog;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.service.dto.BlogViewHistoryDTO;
import com.example.myapp.service.dto.BlogDTO;
import com.example.myapp.service.mapper.BlogMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.vm.BlogStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Service Implementation for managing {@link Blog}.
 */
@Service
@Transactional
public class BlogService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogService.class);

    private final BlogRepository blogRepository;

    private final BlogMapper blogMapper;

    private final BlogViewHistoryService blogViewHistoryService;

    private final AchievementService achievementService;

    public BlogService(BlogRepository blogRepository, BlogMapper blogMapper, BlogViewHistoryService blogViewHistoryService, AchievementService achievementService) {
        this.blogRepository = blogRepository;
        this.blogMapper = blogMapper;
        this.blogViewHistoryService = blogViewHistoryService;
        this.achievementService = achievementService;
    }

    /**
     * Save a blog.
     *
     * @param blogDTO the entity to save.
     * @return the persisted entity.
     */
    public BlogDTO save(BlogDTO blogDTO) {
        LOG.debug("Request to save Blog : {}", blogDTO);
        String currentUsername = SecurityUtil.getCurrentUsername();
        Blog blog = blogMapper.toEntity(blogDTO);
        blog.setCreatedBy(currentUsername);
        blog.setLastModifiedBy(currentUsername);
        if (blog.getVisibility() == null) {
            blog.setVisibility(BlogVisibility.PUBLIC);
        }
        if (isBlank(blog.getAuthor())) {
            blog.setAuthor(currentUsername);
        }
        blog = blogRepository.save(blog);
        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            achievementService.award(blog.getAuthor(), AchievementType.PUBLISH_ARTICLE);
        }
        return blogMapper.toDto(blog);
    }

    /**
     * Update a blog.
     *
     * @param blogDTO the entity to save.
     * @return the persisted entity.
     */
    public BlogDTO update(BlogDTO blogDTO) {
        LOG.debug("Request to update Blog : {}", blogDTO);
        String currentUsername = SecurityUtil.getCurrentUsername();
        Blog existingBlog = blogRepository
            .findById(blogDTO.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!currentUsername.equals(existingBlog.getAuthor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted to update this blog");
        }
        Blog blog = blogMapper.toEntity(blogDTO);
        blog.setAuthor(existingBlog.getAuthor());
        blog.setLastModifiedBy(currentUsername);
        if (blog.getVisibility() == null) {
            blog.setVisibility(BlogVisibility.PUBLIC);
        }
        blog = blogRepository.save(blog);
        return blogMapper.toDto(blog);
    }

    /**
     * Partially update a blog.
     *
     * @param blogDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<BlogDTO> partialUpdate(BlogDTO blogDTO) {
        LOG.debug("Request to partially update Blog : {}", blogDTO);
        String currentUsername = SecurityUtil.getCurrentUsername();

        return blogRepository
            .findById(blogDTO.getId())
            .map(existingBlog -> {
                if (!currentUsername.equals(existingBlog.getAuthor())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted to update this blog");
                }
                String author = existingBlog.getAuthor();
                blogMapper.partialUpdate(existingBlog, blogDTO);
                existingBlog.setAuthor(author);
                existingBlog.setLastModifiedBy(currentUsername);
                if (existingBlog.getVisibility() == null) {
                    existingBlog.setVisibility(BlogVisibility.PUBLIC);
                }
                return existingBlog;
            })
            .map(blogRepository::save)
            .map(blogMapper::toDto);
    }

    /**
     * Get all the blogs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<BlogDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Blogs");
        return blogRepository.findByVisibility(BlogVisibility.PUBLIC, pageable).map(blogMapper::toDto);
    }

    /**
     * Aggregate site-wide stats over public blogs, mirroring the predicate used by
     * {@link #findAll(Pageable)} so the numbers agree with the public blog list.
     *
     * @return count of public blogs plus the sums of their views, likes, and comments.
     */
    @Transactional(readOnly = true)
    public BlogStats getStats() {
        LOG.debug("Request to aggregate public Blog stats");
        Object[] row = blogRepository.aggregateStats(BlogVisibility.PUBLIC).get(0);
        return new BlogStats(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue()
        );
    }

    /**
     * Get current user's blogs.
     *
     * @param pageable the pagination information.
     * @return the list of current user's entities.
     */
    @Transactional(readOnly = true)
    public Page<BlogDTO> findMine(Pageable pageable) {
        LOG.debug("Request to get current user's Blogs");
        String currentUsername = SecurityUtil.getCurrentUsername();
        return blogRepository.findByAuthor(currentUsername, pageable).map(blogMapper::toDto);
    }

    /**
     * Get one blog by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<BlogDTO> findOne(Long id) {
        LOG.debug("Request to get Blog : {}", id);
        return blogRepository.findById(id).map(blogMapper::toDto);
    }

    /**
     * Delete the blog by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Blog : {}", id);
        String currentUsername = SecurityUtil.getCurrentUsername();
        Blog blog = blogRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!currentUsername.equals(blog.getAuthor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted to delete this blog");
        }
        blogRepository.deleteById(id);
    }

    public Optional<BlogViewHistoryDTO> incrementViewCount(Long id) {
        return blogRepository.findById(id).map(blog -> {
            blog.setViewCount((blog.getViewCount() == null ? 0L : blog.getViewCount()) + 1);
            blog.setLastModifiedBy(SecurityUtil.getCurrentUsername());
            Blog saved = blogRepository.save(blog);
            return blogViewHistoryService.recordView(saved);
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
