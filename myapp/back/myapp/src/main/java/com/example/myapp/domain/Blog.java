package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.BlogStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * The blog entity.
 */
@Entity
@Table(name = "twsny_blog")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Blog extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 500)
    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Size(max = 500)
    @Column(name = "summary", length = 500)
    private String summary;

    @Size(max = 512)
    @Column(name = "cover_image", length = 512)
    private String coverImage;

    @Size(max = 100)
    @Column(name = "author", length = 100)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BlogStatus status;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "deleted")
    private Boolean deleted;

    public Long getId() {
        return this.id;
    }

    public Blog id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Blog title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public Blog content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return this.summary;
    }

    public Blog summary(String summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverImage() {
        return this.coverImage;
    }

    public Blog coverImage(String coverImage) {
        this.setCoverImage(coverImage);
        return this;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getAuthor() {
        return this.author;
    }

    public Blog author(String author) {
        this.setAuthor(author);
        return this;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BlogStatus getStatus() {
        return this.status;
    }

    public Blog status(BlogStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(BlogStatus status) {
        this.status = status;
    }

    public Long getViewCount() {
        return this.viewCount;
    }

    public Blog viewCount(Long viewCount) {
        this.setViewCount(viewCount);
        return this;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Boolean getDeleted() {
        return this.deleted;
    }

    public Blog deleted(Boolean deleted) {
        this.setDeleted(deleted);
        return this;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Blog)) {
            return false;
        }
        return getId() != null && getId().equals(((Blog) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Blog{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", summary='" + getSummary() + "'" +
            ", coverImage='" + getCoverImage() + "'" +
            ", author='" + getAuthor() + "'" +
            ", status='" + getStatus() + "'" +
            ", viewCount=" + getViewCount() +
            ", deleted='" + getDeleted() + "'" +
            "}";
    }
}
