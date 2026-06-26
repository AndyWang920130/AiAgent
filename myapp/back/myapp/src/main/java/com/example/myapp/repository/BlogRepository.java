package com.example.myapp.repository;

import com.example.myapp.domain.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Blog entity.
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
}
