package com.example.myapp.repository;

import com.example.myapp.domain.Blog;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogLockRepository extends JpaRepository<Blog, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select blog from Blog blog where blog.id = :id")
    Optional<Blog> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(blog.likes), 0) from Blog blog where blog.author = :username")
    Long sumLikesByAuthor(@Param("username") String username);
}
