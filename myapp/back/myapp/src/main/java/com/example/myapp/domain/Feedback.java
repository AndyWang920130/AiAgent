package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.FeedbackCategory;
import com.example.myapp.contants.enumeration.FeedbackStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * One piece of feedback sent to the administrators. The inherited {@code createdDate} is the
 * submission time; {@code username} is the account that sent it, kept as the login rather
 * than a relation so a submission survives the account being removed.
 *
 * <p>{@code reply} and its two companions are filled in when an administrator answers. They
 * stay null for an unanswered submission, which is how the client tells the two apart.
 */
@Entity
@Table(
    name = "twsny_feedback",
    indexes = {
        @Index(name = "idx_twsny_feedback_username", columnList = "username"),
        @Index(name = "idx_twsny_feedback_status", columnList = "status")
    }
)
public class Feedback extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Size(max = 100)
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private FeedbackCategory category;

    @NotNull @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotNull @Size(max = 2000)
    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    /** Optional way to reach the submitter outside the app — an email or phone they typed. */
    @Size(max = 200)
    @Column(name = "contact", length = 200)
    private String contact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Size(max = 2000)
    @Column(name = "reply", length = 2000)
    private String reply;

    @Size(max = 100)
    @Column(name = "replied_by", length = 100)
    private String repliedBy;

    @Column(name = "replied_date")
    private Instant repliedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public FeedbackCategory getCategory() { return category; }
    public void setCategory(FeedbackCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getRepliedBy() { return repliedBy; }
    public void setRepliedBy(String repliedBy) { this.repliedBy = repliedBy; }
    public Instant getRepliedDate() { return repliedDate; }
    public void setRepliedDate(Instant repliedDate) { this.repliedDate = repliedDate; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Feedback other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
