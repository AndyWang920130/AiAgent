package com.example.myapp.service;

import com.example.myapp.domain.Blog;
import com.example.myapp.domain.BlogComment;
import com.example.myapp.repository.BlogCommentRepository;
import com.example.myapp.repository.BlogLockRepository;
import com.example.myapp.service.dto.BlogCommentDTO;
import com.example.myapp.utils.SecurityUtil;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BlogCommentService {
    private final BlogLockRepository blogRepository;
    private final BlogCommentRepository commentRepository;

    public BlogCommentService(BlogLockRepository blogRepository, BlogCommentRepository commentRepository) {
        this.blogRepository = blogRepository;
        this.commentRepository = commentRepository;
    }

    public Optional<BlogCommentDTO> create(Long blogId, String content) {
        String username = SecurityUtil.getCurrentUsername();
        return blogRepository.findByIdForUpdate(blogId).map(blog -> {
            BlogComment comment = new BlogComment();
            comment.setUsername(username);
            comment.setBlog(blog);
            comment.setContent(content);
            comment.setCreatedBy(username);
            comment.setLastModifiedBy(username);
            commentRepository.save(comment);

            blog.setCommentCount(commentCount(blog) + 1);
            blog.setLastModifiedBy(username);
            blogRepository.save(blog);
            return toDto(comment, blog, username);
        });
    }

    public void delete(Long commentId) {
        String username = SecurityUtil.getCurrentUsername();
        BlogComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Blog blog = blogRepository.findByIdForUpdate(comment.getBlog().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAuthor = comment.getUsername().equals(username);
        boolean isBlogOwner = username.equals(blog.getAuthor());
        if (!isAuthor && !isBlogOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted to delete this comment");
        }

        commentRepository.delete(comment);
        blog.setCommentCount(Math.max(0, commentCount(blog) - 1));
        blog.setLastModifiedBy(username);
        blogRepository.save(blog);
    }

    @Transactional(readOnly = true)
    public Optional<Page<BlogCommentDTO>> list(Long blogId, Pageable pageable) {
        String username = SecurityUtil.getCurrentUsername();
        return blogRepository.findById(blogId).map(blog ->
            commentRepository.findByBlogIdOrderByCreatedDateAsc(blogId, pageable).map(comment -> toDto(comment, blog, username))
        );
    }

    private long commentCount(Blog blog) { return blog.getCommentCount() == null ? 0L : blog.getCommentCount(); }

    private BlogCommentDTO toDto(BlogComment comment, Blog blog, String currentUsername) {
        boolean canDelete = comment.getUsername().equals(currentUsername) || currentUsername.equals(blog.getAuthor());
        return new BlogCommentDTO(
            comment.getId(), blog.getId(), comment.getUsername(), comment.getContent(),
            comment.getCreatedDate(), canDelete
        );
    }
}
