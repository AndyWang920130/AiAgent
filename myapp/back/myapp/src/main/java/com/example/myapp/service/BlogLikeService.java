package com.example.myapp.service;

import com.example.myapp.domain.Blog;
import com.example.myapp.domain.BlogLikeHistory;
import com.example.myapp.repository.BlogLikeHistoryRepository;
import com.example.myapp.repository.BlogLockRepository;
import com.example.myapp.service.dto.BlogLikeHistoryDTO;
import com.example.myapp.service.dto.BlogLikeStatusDTO;
import com.example.myapp.utils.SecurityUtil;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlogLikeService {
    private final BlogLockRepository blogRepository;
    private final BlogLikeHistoryRepository historyRepository;

    public BlogLikeService(BlogLockRepository blogRepository, BlogLikeHistoryRepository historyRepository) {
        this.blogRepository = blogRepository;
        this.historyRepository = historyRepository;
    }

    public Optional<BlogLikeStatusDTO> like(Long blogId) {
        String username = SecurityUtil.getCurrentUsername();
        return blogRepository.findByIdForUpdate(blogId).map(blog -> {
            Optional<BlogLikeHistory> existing = historyRepository.findByUsernameAndBlogId(username, blogId);
            if (existing.isPresent()) return status(blog, existing.get());

            Instant now = Instant.now();
            BlogLikeHistory history = new BlogLikeHistory();
            history.setUsername(username);
            history.setBlog(blog);
            history.setLikedDate(now);
            history.setCreatedBy(username);
            history.setLastModifiedBy(username);
            historyRepository.save(history);

            blog.setLikes(likes(blog) + 1);
            blog.setLastModifiedBy(username);
            blogRepository.save(blog);
            return status(blog, history);
        });
    }

    @Transactional(readOnly = true)
    public Optional<BlogLikeStatusDTO> getStatus(Long blogId) {
        String username = SecurityUtil.getCurrentUsername();
        return blogRepository.findById(blogId).map(blog ->
            historyRepository.findByUsernameAndBlogId(username, blogId)
                .map(history -> status(blog, history))
                .orElseGet(() -> new BlogLikeStatusDTO(blogId, likes(blog), false, null))
        );
    }

    @Transactional(readOnly = true)
    public Page<BlogLikeHistoryDTO> findMine(Pageable pageable) {
        String username = SecurityUtil.getCurrentUsername();
        return historyRepository.findByUsernameOrderByLikedDateDesc(username, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getLikesReceived() {
        Long total = blogRepository.sumLikesByAuthor(SecurityUtil.getCurrentUsername());
        return total == null ? 0L : total;
    }

    private BlogLikeStatusDTO status(Blog blog, BlogLikeHistory history) {
        return new BlogLikeStatusDTO(blog.getId(), likes(blog), true, history.getLikedDate());
    }

    private long likes(Blog blog) { return blog.getLikes() == null ? 0L : blog.getLikes(); }

    private BlogLikeHistoryDTO toDto(BlogLikeHistory history) {
        Blog blog = history.getBlog();
        return new BlogLikeHistoryDTO(
            history.getId(), history.getUsername(), blog.getId(), blog.getTitle(),
            blog.getAuthor(), likes(blog), history.getLikedDate()
        );
    }
}
