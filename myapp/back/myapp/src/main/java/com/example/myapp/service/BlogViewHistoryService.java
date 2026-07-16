package com.example.myapp.service;

import com.example.myapp.domain.Blog;
import com.example.myapp.domain.BlogViewHistory;
import com.example.myapp.repository.BlogViewHistoryRepository;
import com.example.myapp.service.dto.BlogViewHistoryDTO;
import com.example.myapp.utils.SecurityUtil;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlogViewHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogViewHistoryService.class);

    private final BlogViewHistoryRepository blogViewHistoryRepository;

    public BlogViewHistoryService(BlogViewHistoryRepository blogViewHistoryRepository) {
        this.blogViewHistoryRepository = blogViewHistoryRepository;
    }

    public BlogViewHistoryDTO recordView(Blog blog) {
        String username = SecurityUtil.getCurrentUsername();
        Instant now = Instant.now();
        BlogViewHistory history = blogViewHistoryRepository
            .findByUsernameAndBlogId(username, blog.getId())
            .orElseGet(() -> {
                BlogViewHistory created = new BlogViewHistory();
                created.setUsername(username);
                created.setBlog(blog);
                created.setViewCount(0L);
                created.setFirstViewedDate(now);
                created.setCreatedBy(username);
                return created;
            });

        history.setViewCount((history.getViewCount() == null ? 0L : history.getViewCount()) + 1);
        history.setLastViewedDate(now);
        history.setLastModifiedBy(username);
        return toDto(blogViewHistoryRepository.save(history));
    }

    @Transactional(readOnly = true)
    public Page<BlogViewHistoryDTO> findMine(Pageable pageable) {
        String username = SecurityUtil.getCurrentUsername();
        LOG.debug("Request to get blog view history for user : {}", username);
        return blogViewHistoryRepository.findByUsernameOrderByLastViewedDateDesc(username, pageable).map(this::toDto);
    }

    private BlogViewHistoryDTO toDto(BlogViewHistory history) {
        Blog blog = history.getBlog();
        BlogViewHistoryDTO dto = new BlogViewHistoryDTO();
        dto.setId(history.getId());
        dto.setUsername(history.getUsername());
        dto.setBlogId(blog.getId());
        dto.setBlogTitle(blog.getTitle());
        dto.setBlogAuthor(blog.getAuthor());
        dto.setBlogViewCount(blog.getViewCount());
        dto.setViewCount(history.getViewCount());
        dto.setFirstViewedDate(history.getFirstViewedDate());
        dto.setLastViewedDate(history.getLastViewedDate());
        return dto;
    }
}
