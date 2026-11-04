package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
    public CardGameInvitationStatus getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(CardGameInvitationStatus invitationStatus) { this.invitationStatus = invitationStatus; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CardGamePlayer other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
