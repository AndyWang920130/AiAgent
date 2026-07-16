package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Records when a user liked a blog. A user can like a blog only once. */
@Entity
@Table(
    name = "twsny_blog_like_history",
    uniqueConstraints = @UniqueConstraint(name = "uk_twsny_blog_like_history_user_blog", columnNames = {"username", "blog_id"}),
    indexes = {
        @Index(name = "idx_twsny_blog_like_history_username", columnList = "username"),
        @Index(name = "idx_twsny_blog_like_history_blog_id", columnList = "blog_id"),
        @Index(name = "idx_twsny_blog_like_history_liked_date", columnList = "liked_date")
    }
)
public class BlogLikeHistory extends AbstractAuditingEntity {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @Size(max = 100) @Column(nullable = false, length = 100)
    private String username;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;
    @NotNull @Column(name = "liked_date", nullable = false)
    private Instant likedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Blog getBlog() { return blog; }
    public void setBlog(Blog blog) { this.blog = blog; }
    public Instant getLikedDate() { return likedDate; }
    public void setLikedDate(Instant likedDate) { this.likedDate = likedDate; }
    @Override public boolean equals(Object o) {
        return this == o || o instanceof BlogLikeHistory other && getId() != null && getId().equals(other.getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
