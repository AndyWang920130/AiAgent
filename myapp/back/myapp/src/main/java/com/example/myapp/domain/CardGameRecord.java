package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.CardGameRecordOutcome;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One finished 70235 match, kept for the administrators' match record list.
 *
 * <p>This table exists because {@link CardGame} is the <em>room</em>, not the match. A rematch
 * re-deals the same row: {@code CardGameService.deal} sets every seat's score back to 0,
 * re-stamps {@code startedDate} and clears the winner. So the room row only ever holds the most
 * recent game, and the one before it is gone. A match that is to be listed later therefore has
 * to be snapshotted when it ends, which is what this is.
 *
 * <p>A snapshot, so the values here are copies and do not track the room afterwards. Only
 * logins are stored, never display names — a name can be edited and the list should show the
 * current one, so names are resolved at read time.
 */
@Entity
@Table(
    name = "twsny_card_game_record",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_twsny_card_game_record_deal",
        columnNames = { "game_id", "started_date" }
    ),
    indexes = {
        @Index(name = "idx_twsny_card_game_record_game", columnList = "game_id"),
        @Index(name = "idx_twsny_card_game_record_host", columnList = "host_username"),
        @Index(name = "idx_twsny_card_game_record_ended", columnList = "ended_date")
    }
)
public class CardGameRecord extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The room this match was played in. A plain column rather than a {@code @ManyToOne}: the
     * record has to outlive the room, and a relation would invite a cascade that deleted
     * history along with it.
     */
    @NotNull
    @Column(name = "game_id", nullable = false)
    private Long gameId;

    /** Who created the table. */
    @NotNull @Size(max = 100)
    @Column(name = "host_username", nullable = false, length = 100)
    private String hostUsername;

    /** When the deal happened, copied from {@code CardGame.startedDate}. */
    @NotNull
    @Column(name = "started_date", nullable = false)
    private Instant startedDate;

    /**
     * When the match ended. There is no such column on the room, so this is the only place the
     * end of a match is recorded. Supplied by the caller rather than defaulted to "now": a
     * match stopped by the round cap ends when the cap is reached, which can be well before
     * anything reads the row and notices.
     */
    @NotNull
    @Column(name = "ended_date", nullable = false)
    private Instant endedDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private CardGameRecordOutcome outcome;

    /** Highest final score. Null on an ABANDONED match, which never reached scoring. */
    @Size(max = 100)
    @Column(name = "winner_username", length = 100)
    private String winnerUsername;

    /** First player to empty their hand, or null if nobody did. */
    @Size(max = 100)
    @Column(name = "first_out_username", length = 100)
    private String firstOutUsername;

    /** Who walked out. Set on an ABANDONED match and null otherwise. */
    @Size(max = 100)
    @Column(name = "left_by_username", length = 100)
    private String leftByUsername;

    /** Tricks played. Copied so the list can show how far the match got. */
    @Column(name = "trick_count", nullable = false)
    private int trickCount = 0;

    /**
     * Seats that played. Denormalised from {@link #players} so the list can show and sort by
     * it without joining — the column is one of the few a match record list is sorted on.
     */
    @Column(name = "player_count", nullable = false)
    private int playerCount = 0;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seat asc")
    private List<CardGameRecordPlayer> players = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getHostUsername() { return hostUsername; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
    public Instant getStartedDate() { return startedDate; }
    public void setStartedDate(Instant startedDate) { this.startedDate = startedDate; }
    public Instant getEndedDate() { return endedDate; }
    public void setEndedDate(Instant endedDate) { this.endedDate = endedDate; }
    public CardGameRecordOutcome getOutcome() { return outcome; }
    public void setOutcome(CardGameRecordOutcome outcome) { this.outcome = outcome; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
    public String getFirstOutUsername() { return firstOutUsername; }
    public void setFirstOutUsername(String firstOutUsername) { this.firstOutUsername = firstOutUsername; }
    public String getLeftByUsername() { return leftByUsername; }
    public void setLeftByUsername(String leftByUsername) { this.leftByUsername = leftByUsername; }
    public int getTrickCount() { return trickCount; }
    public void setTrickCount(int trickCount) { this.trickCount = trickCount; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public List<CardGameRecordPlayer> getPlayers() { return players; }
    public void setPlayers(List<CardGameRecordPlayer> players) { this.players = players; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CardGameRecord other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
