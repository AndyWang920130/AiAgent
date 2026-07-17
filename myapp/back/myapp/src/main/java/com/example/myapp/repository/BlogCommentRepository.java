package com.example.myapp.repository;

import com.example.myapp.domain.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    Page<BlogComment> findByBlogIdOrderByCreatedDateAsc(Long blogId, Pageable pageable);
}
