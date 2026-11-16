package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.ReleaseNoteStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * One release's notes, written by an administrator and read by everybody — the About section's
 * changelog.
 *
 * <p>The inherited audit fields carry the authorship this page needs, so none of it is
 * duplicated here: {@code createdBy} is who wrote the note, {@code lastModifiedBy} and
 * {@code lastModifiedDate} are who last edited it and when. Only the login is kept, never a
 * display name — a name can be changed afterwards and the page should show the current one, so
 * names are resolved at read time.
 */
@Entity
@Table(
    name = "twsny_release_note",
    uniqueConstraints = @UniqueConstraint(name = "ux_twsny_release_note_version", columnNames = "version"),
    indexes = @Index(name = "idx_twsny_release_note_status_released", columnList = "status, released_date")
)
public class ReleaseNote extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The release this describes, as the administrator types it ({@code 1.4.0}, {@code 2026.11}
     * — no format is imposed). Unique: two rows claiming the same release is a mistake worth a
     * failed save rather than a page that lists it twice.
     */
    @NotNull @Size(max = 50)
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    /** Optional headline for the release. The version alone is a fine entry without one. */
    @Size(max = 200)
    @Column(name = "title", length = 200)
    private String title;

    /**
     * The note itself, as HTML from the rich editor. {@code longtext} like {@code Blog.content}
     * — a release note has no useful length limit, and a varchar cap would be discovered by
     * truncating somebody's work.
     */
    @Lob
    @NotNull
    @Column(name = "content", nullable = false, columnDefinition = "longtext")
    private String content;

    /**
     * When the release happened. Deliberately not the inherited {@code createdDate}, which is
     * when the note was typed: notes are often written a little before or after the release
     * they describe, and the date the page is ordered by should be the release's.
     */
    @NotNull
    @Column(name = "released_date", nullable = false)
    private Instant releasedDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReleaseNoteStatus status = ReleaseNoteStatus.DRAFT;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getReleasedDate() { return releasedDate; }
    public void setReleasedDate(Instant releasedDate) { this.releasedDate = releasedDate; }
    public ReleaseNoteStatus getStatus() { return status; }
    public void setStatus(ReleaseNoteStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ReleaseNote other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
