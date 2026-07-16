package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

public class BlogViewHistoryDTO implements Serializable {

    private Long id;

    private String username;

    private Long blogId;

    private String blogTitle;

    private String blogAuthor;

    private Long blogViewCount;

    private Long viewCount;

    private Instant firstViewedDate;

    private Instant lastViewedDate;

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

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getBlogAuthor() {
        return blogAuthor;
    }

    public void setBlogAuthor(String blogAuthor) {
        this.blogAuthor = blogAuthor;
    }

    public Long getBlogViewCount() {
        return blogViewCount;
    }

    public void setBlogViewCount(Long blogViewCount) {
        this.blogViewCount = blogViewCount;
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
}
