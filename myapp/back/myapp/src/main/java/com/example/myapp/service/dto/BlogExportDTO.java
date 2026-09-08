package com.example.myapp.service.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

import java.io.Serializable;

/**
 * Flat row model used exclusively for exporting blogs to Excel via EasyExcel.
 *
 * <p>This is intentionally separate from {@link BlogDTO}: it omits the large
 * {@code content} column, orders columns explicitly for a spreadsheet, and renders
 * enums/dates as plain strings so the output is human-readable. Column order is
 * fixed via the {@code index} attribute so it never depends on field declaration order.
 */
public class BlogExportDTO implements Serializable {

    @ColumnWidth(10)
    @ExcelProperty(value = "ID", index = 0)
    private Long id;

    @ColumnWidth(40)
    @ExcelProperty(value = "Title", index = 1)
    private String title;

    @ColumnWidth(20)
    @ExcelProperty(value = "Author", index = 2)
    private String author;

    @ColumnWidth(15)
    @ExcelProperty(value = "Category", index = 3)
    private String category;

    @ColumnWidth(15)
    @ExcelProperty(value = "Tag", index = 4)
    private String tag;

    @ColumnWidth(12)
    @ExcelProperty(value = "Status", index = 5)
    private String status;

    @ColumnWidth(12)
    @ExcelProperty(value = "Visibility", index = 6)
    private String visibility;

    @ColumnWidth(12)
    @ExcelProperty(value = "Views", index = 7)
    private Long viewCount;

    @ColumnWidth(12)
    @ExcelProperty(value = "Likes", index = 8)
    private Long likes;

    @ColumnWidth(12)
    @ExcelProperty(value = "Comments", index = 9)
    private Long commentCount;

    @ColumnWidth(20)
    @ExcelProperty(value = "Created By", index = 10)
    private String createdBy;

    @ColumnWidth(22)
    @ExcelProperty(value = "Created Date", index = 11)
    private String createdDate;

    @ColumnWidth(22)
    @ExcelProperty(value = "Last Modified Date", index = 12)
    private String lastModifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
