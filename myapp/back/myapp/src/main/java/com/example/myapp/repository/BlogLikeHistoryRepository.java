package com.example.myapp.repository;

import com.example.myapp.domain.BlogLikeHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogLikeHistoryRepository extends JpaRepository<BlogLikeHistory, Long> {
    Optional<BlogLikeHistory> findByUsernameAndBlogId(String username, Long blogId);
    Page<BlogLikeHistory> findByUsernameOrderByLikedDateDesc(String username, Pageable pageable);
}
