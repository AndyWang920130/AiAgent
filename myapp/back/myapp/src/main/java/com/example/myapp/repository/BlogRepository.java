package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.domain.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Blog entity.
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByVisibility(BlogVisibility visibility, Pageable pageable);

    Page<Blog> findByCreatedBy(String createdBy, Pageable pageable);

    Page<Blog> findByAuthor(String author, Pageable pageable);

    Page<Blog> findByAuthorAndVisibility(String author, BlogVisibility visibility, Pageable pageable);

    long countByAuthorAndVisibility(String author, BlogVisibility visibility);

    /**
     * Aggregate site-wide stats over blogs matching the given visibility, mirroring
     * the predicate used by {@code findByVisibility}. Returns a single row (element 0):
     * [count, sum(viewCount), sum(likes), sum(commentCount)], with nulls coalesced to 0.
     */
    @Query("""
        select count(b), coalesce(sum(b.viewCount), 0), coalesce(sum(b.likes), 0), coalesce(sum(b.commentCount), 0)
        from Blog b
        where b.visibility = :visibility
        """)
    List<Object[]> aggregateStats(@Param("visibility") BlogVisibility visibility);
}
