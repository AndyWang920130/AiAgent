package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Records a comment left on a blog by a user. */
@Entity
@Table(
    name = "twsny_blog_comment",
    indexes = {
        @Index(name = "idx_twsny_blog_comment_blog_id", columnList = "blog_id"),
        @Index(name = "idx_twsny_blog_comment_created_date", columnList = "created_date")
    }
)
public class BlogComment extends AbstractAuditingEntity {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @Size(max = 100) @Column(nullable = false, length = 100)
    private String username;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;
    @NotNull @Lob @Column(nullable = false, columnDefinition = "text")
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Blog getBlog() { return blog; }
    public void setBlog(Blog blog) { this.blog = blog; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    @Override public boolean equals(Object o) {
        return this == o || o instanceof BlogComment other && getId() != null && getId().equals(other.getId());
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
