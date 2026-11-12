package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGamePhrase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * One seat at a {@link CardGame} table — a human player or a CPU opponent.
 *
 * <p>{@link #hand} is private game state. It is only ever serialised to its own owner (see
 * the per-recipient DTO in {@code CardGameService}); no DTO carries another player's cards.
 */
@Entity
@Table(
    name = "twsny_card_game_player",
    uniqueConstraints = @UniqueConstraint(name = "ux_twsny_card_game_player", columnNames = { "game_id", "username" }),
    indexes = {
        @Index(name = "idx_twsny_card_game_player_game", columnList = "game_id"),
        @Index(name = "idx_twsny_card_game_player_user", columnList = "username")
    }
)
public class CardGamePlayer extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private CardGame game;

    /** A real login, or a synthetic {@code cpu-1}..{@code cpu-4} for a CPU seat. */
    @NotNull @Size(max = 100)
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** 0-based seat. Fixes both turn order and the refill draw order. */
    @Column(name = "seat", nullable = false)
    private int seat;

    /** This player's cards, two characters each. Never sent to another player. */
    @Size(max = 255)
    @Column(name = "hand", length = 255)
    private String hand = "";

    /** Point cards captured so far, as a running total. */
    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "cpu", nullable = false)
    private boolean cpu = false;

    /**
     * This player has asked for their turns to be taken for them. Per seat and per game, so it
     * lapses with the table rather than following the user to the next one.
     *
     * <p>Only meaningful on a human seat: a CPU seat plays itself regardless.
     */
    @Column(name = "auto_play", nullable = false)
    private boolean autoPlay = false;

    /**
     * Consecutive turns the server has taken for this seat because its clock ran out. Reset by
     * any turn the player takes themselves, and by either direction of the auto-play switch —
     * resetting is what makes the count consecutive rather than a lifetime total.
     *
     * <p>At the threshold the service switches {@code autoPlay} on, on the grounds that somebody
     * who has let two turns in a row be played for them is away from the table, and a seat the
     * rest of the table waits out one full clock per turn for is a table nobody can finish.
     *
     * <p>A count rather than a "has missed one" flag: the threshold is a number with a reason,
     * and a boolean would need renaming the first time that number is not two. Per seat and per
     * game, like {@code autoPlay} above.
     *
     * <p>Only meaningful on a human seat playing its own turns: a CPU seat, and a seat already
     * on auto-play, are played by design rather than by default, so neither is counted.
     */
    @Column(name = "timeout_streak", nullable = false)
    private int timeoutStreak = 0;

    /**
     * This player has said they want the next game, while the room sits in
     * {@code CardGameStatus.REMATCH}. Reset by every deal, so it describes the game about to
     * start rather than the one just played.
     *
     * <p>Deliberately not {@code invitationStatus}. Reusing that would make "has not readied up
     * yet" and "has not answered an invitation yet" the same value, and a deal drops every
     * DECLINED seat and counts the ACCEPTED ones — so a player who was merely slow to ready up
     * would be at risk of being treated as one who had turned the table down. Readiness is its
     * own question and gets its own column.
     *
     * <p>Only meaningful on a non-host human seat: a CPU has nothing to decide, and the host's
     * readiness is them dealing.
     */
    @Column(name = "ready", nullable = false)
    private boolean ready = false;

    /**
     * The last phrase this seat sent, or null for a seat that has not said anything.
     *
     * <p>Nullable with no default, unlike the flags above: "has said nothing" is the absence of a
     * phrase rather than a value one of them holds.
     *
     * <p>Only the most recent one is kept. The table draws a phrase as a bubble on its sender's
     * seat, so the latest is the whole of what anyone ever reads — there is no log to trim and no
     * encoded list to decode, which is what this would need if the phrases were shown as a
     * conversation instead.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phrase", length = 20)
    private CardGamePhrase phrase;

    /**
     * When {@link #phrase} was sent. Drives two things, both of them the server's to decide: how
     * long the phrase is reported for, and how soon this seat may send another.
     */
    @Column(name = "phrase_date")
    private Instant phraseDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 20)
    private CardGameInvitationStatus invitationStatus = CardGameInvitationStatus.PENDING;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CardGame getGame() { return game; }
    public void setGame(CardGame game) { this.game = game; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getSeat() { return seat; }
    public void setSeat(int seat) { this.seat = seat; }
    public String getHand() { return hand; }
    public void setHand(String hand) { this.hand = hand; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isCpu() { return cpu; }
    public void setCpu(boolean cpu) { this.cpu = cpu; }
    public boolean isAutoPlay() { return autoPlay; }
    public void setAutoPlay(boolean autoPlay) { this.autoPlay = autoPlay; }

    public int getTimeoutStreak() { return timeoutStreak; }
    public void setTimeoutStreak(int timeoutStreak) { this.timeoutStreak = timeoutStreak; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public CardGamePhrase getPhrase() { return phrase; }
    public void setPhrase(CardGamePhrase phrase) { this.phrase = phrase; }
    public Instant getPhraseDate() { return phraseDate; }
    public void setPhraseDate(Instant phraseDate) { this.phraseDate = phraseDate; }
    public CardGameInvitationStatus getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(CardGameInvitationStatus invitationStatus) { this.invitationStatus = invitationStatus; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CardGamePlayer other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
