package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.domain.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Blog entity.
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByVisibility(BlogVisibility visibility, Pageable pageable);

    Page<Blog> findByCreatedBy(String createdBy, Pageable pageable);
}
