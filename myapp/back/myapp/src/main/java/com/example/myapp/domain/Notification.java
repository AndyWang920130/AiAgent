package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A per-user notification. The inherited {@code createdDate} is the notification's timestamp.
 * {@code link} is an optional in-app path the client navigates to when the item is tapped.
 */
@Entity
@Table(
    name = "twsny_notification",
    indexes = {
        @Index(name = "idx_twsny_notification_recipient", columnList = "recipient"),
        @Index(name = "idx_twsny_notification_recipient_read", columnList = "recipient, is_read")
    }
)
public class Notification extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Size(max = 100)
    @Column(name = "recipient", nullable = false, length = 100)
    private String recipient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @NotNull @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 500)
    @Column(name = "content", length = 500)
    private String content;

    @Size(max = 200)
    @Column(name = "link", length = 200)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Notification other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
