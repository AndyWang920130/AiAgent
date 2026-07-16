package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Aggregated blog view history for a single user and blog.
 */
@Entity
@Table(
    name = "twsny_blog_view_history",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_twsny_blog_view_history_user_blog", columnNames = {"username", "blog_id"})
    },
    indexes = {
        @Index(name = "idx_twsny_blog_view_history_username", columnList = "username"),
        @Index(name = "idx_twsny_blog_view_history_blog_id", columnList = "blog_id"),
        @Index(name = "idx_twsny_blog_view_history_last_viewed_date", columnList = "last_viewed_date")
    }
)
public class BlogViewHistory extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @NotNull
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @NotNull
    @Column(name = "first_viewed_date", nullable = false)
    private Instant firstViewedDate = Instant.now();

    @NotNull
    @Column(name = "last_viewed_date", nullable = false)
    private Instant lastViewedDate = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Instant getFirstViewedDate() {
        return firstViewedDate;
    }

    public void setFirstViewedDate(Instant firstViewedDate) {
        this.firstViewedDate = firstViewedDate;
    }

    public Instant getLastViewedDate() {
        return lastViewedDate;
    }

    public void setLastViewedDate(Instant lastViewedDate) {
        this.lastViewedDate = lastViewedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogViewHistory)) {
            return false;
        }
        return getId() != null && getId().equals(((BlogViewHistory) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
