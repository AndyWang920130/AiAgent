package com.example.myapp.repository;

import com.example.myapp.domain.BlogViewHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogViewHistoryRepository extends JpaRepository<BlogViewHistory, Long> {

    Optional<BlogViewHistory> findByUsernameAndBlogId(String username, Long blogId);

    Page<BlogViewHistory> findByUsernameOrderByLastViewedDateDesc(String username, Pageable pageable);
}
