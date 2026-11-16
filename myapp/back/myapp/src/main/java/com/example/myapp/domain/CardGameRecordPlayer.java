package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One seat's result in a recorded 70235 match — the per-user score the administrators' list
 * shows.
 *
 * <p>A row per seat rather than a joined "alice:62,bob:31" string on {@link CardGameRecord}.
 * Scores are the point of this feature, and a string would have to be parsed to answer
 * anything about them; as rows they can be filtered and aggregated by the database.
 *
 * <p>No hand is recorded. The match is over, so the cards hold no interest, and a hand is
 * private state that has no business being copied anywhere an administrator can read it.
 */
@Entity
@Table(
    name = "twsny_card_game_record_player",
    indexes = {
        @Index(name = "idx_twsny_card_game_record_player_record", columnList = "record_id"),
        @Index(name = "idx_twsny_card_game_record_player_user", columnList = "username")
    }
)
public class CardGameRecordPlayer extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private CardGameRecord record;

    /** A real login, or a synthetic {@code cpu-1}..{@code cpu-4} for a CPU seat. */
    @NotNull @Size(max = 100)
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** 0-based seat, as it was at the deal. Fixes the order the scores are listed in. */
    @Column(name = "seat", nullable = false)
    private int seat;

    /** This seat's score when the match ended. */
    @Column(name = "score", nullable = false)
    private int score = 0;

    /**
     * A CPU seat. Nothing creates these any more, but tables played before they were withdrawn
     * had them, so a match recorded from such a room needs to be able to say so.
     */
    @Column(name = "cpu", nullable = false)
    private boolean cpu = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CardGameRecord getRecord() { return record; }
    public void setRecord(CardGameRecord record) { this.record = record; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getSeat() { return seat; }
    public void setSeat(int seat) { this.seat = seat; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isCpu() { return cpu; }
    public void setCpu(boolean cpu) { this.cpu = cpu; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CardGameRecordPlayer other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
