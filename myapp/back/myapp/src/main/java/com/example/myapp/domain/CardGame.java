package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.CardGameStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An online 70235 card game for 2-5 players. A single row models the table, its invitation
 * phase and the match: it starts PENDING while invitees respond, becomes ACTIVE when the host
 * starts the deal, then terminal.
 *
 * <p>The server owns the deck and every hand. Hands live on {@link CardGamePlayer} and are
 * only ever sent to their owner, so a client cannot see what anyone else holds.
 *
 * <p>Cards are two characters each — see {@code CardDeckService} for the encoding — so every
 * card string here is read by chunking into pairs.
 */
@Entity
@Table(
    name = "twsny_card_game",
    indexes = {
        @Index(name = "idx_twsny_card_game_host", columnList = "host_username"),
        @Index(name = "idx_twsny_card_game_status", columnList = "status")
    }
)
public class CardGame extends AbstractAuditingEntity {

    /** Hand size every player refills to while the deck lasts. */
    public static final int TARGET_HAND_SIZE = 5;

    /** The player who leads the first trick is dealt one extra card. */
    public static final int LEAD_HAND_SIZE = TARGET_HAND_SIZE + 1;

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardGameStatus status;

    /** Table owner: sets the seats up, clicks Start, and leads the first trick. */
    @NotNull @Size(max = 100)
    @Column(name = "host_username", nullable = false, length = 100)
    private String hostUsername;

    /** Undealt cards. 108 cards x 2 chars = 216, so 255 is the bound. */
    @Size(max = 255)
    @Column(name = "deck", length = 255)
    private String deck = "";

    /** The play currently on the table (the one to beat), or null on a fresh trick. */
    @Size(max = 64)
    @Column(name = "current_play", length = 64)
    private String currentPlay;

    /** Who played {@link #currentPlay}. They win the trick if everyone else passes. */
    @Size(max = 100)
    @Column(name = "current_play_username", length = 100)
    private String currentPlayUsername;

    /**
     * Shape of {@link #currentPlay}: SINGLE, PAIR, TRIPLE, QUAD, FULL_HOUSE or QUAD_PLUS_ONE.
     * The longest of those is 13 characters, inside the column width.
     */
    @Size(max = 16)
    @Column(name = "current_shape", length = 16)
    private String currentShape;

    /** Strength index of the play to beat (the triple's/quad's rank for the 5-card shapes). */
    @Column(name = "current_rank")
    private Integer currentRank;

    /** Every card played this trick, so the trick winner can be awarded its point cards. */
    @Size(max = 512)
    @Column(name = "trick_pile", length = 512)
    private String trickPile = "";

    /**
     * Comma-joined usernames that have declined the play currently on the table.
     *
     * <p>Display only: it marks who has passed on <em>this</em> play, not who is out of the
     * trick. A pass declines one play, so a raise clears this — the turn then comes back round
     * to everybody, passers included, to answer the new play.
     */
    @Size(max = 512)
    @Column(name = "passed_usernames", length = 512)
    private String passedUsernames = "";

    /**
     * What happened this trick, in order, so clients can show a lead being raised rather than
     * only the play currently on top. Entries are {@code seat:cards} (or {@code seat:-} for a
     * pass) joined by {@code |} — for example {@code 0:8S|1:9S|2:TS}.
     *
     * <p>Seat indexes rather than usernames keep it short: a raise must strictly increase in
     * rank, so a trick holds at most one play per rank plus one pass per seat.
     *
     * <p>Deliberately <strong>not</strong> cleared when a trick is scored. The play that wins a
     * trick is its last entry, so clearing on resolution would delete the winning play at the
     * moment it won and leave the next trick's lead as the first thing a client ever sees.
     * {@link #trickLogNumber} records which trick the log describes; it is replaced, rather than
     * emptied, when the next trick's first action arrives.
     */
    @Size(max = 512)
    @Column(name = "trick_log", length = 512)
    private String trickLog = "";

    /** The trick {@link #trickLog} describes, which lags {@link #trickNumber} once one is scored. */
    @Column(name = "trick_log_number", nullable = false)
    private int trickLogNumber = 0;

    /** Whose action is due while ACTIVE. */
    @Size(max = 100)
    @Column(name = "current_turn_username", length = 100)
    private String currentTurnUsername;

    @Column(name = "trick_number", nullable = false)
    private int trickNumber = 0;

    /** First player to empty their hand. Collects the point cards left in every other hand. */
    @Size(max = 100)
    @Column(name = "first_out_username", length = 100)
    private String firstOutUsername;

    /** Highest final score, once FINISHED. */
    @Size(max = 100)
    @Column(name = "winner_username", length = 100)
    private String winnerUsername;

    /** Player who left the table. Used to close the other players' UI too. */
    @Size(max = 100)
    @Column(name = "left_by_username", length = 100)
    private String leftByUsername;

    /** When the deal happened (drives the whole-game timeout). Null while PENDING. */
    @Column(name = "started_date")
    private Instant startedDate;

    /** When the current turn's clock started — set on start and on every play/pass. */
    @Column(name = "last_action_date")
    private Instant lastActionDate;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seat asc")
    private List<CardGamePlayer> players = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CardGameStatus getStatus() { return status; }
    public void setStatus(CardGameStatus status) { this.status = status; }
    public String getHostUsername() { return hostUsername; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
    public String getDeck() { return deck; }
    public void setDeck(String deck) { this.deck = deck; }
    public String getCurrentPlay() { return currentPlay; }
    public void setCurrentPlay(String currentPlay) { this.currentPlay = currentPlay; }
    public String getCurrentPlayUsername() { return currentPlayUsername; }
    public void setCurrentPlayUsername(String currentPlayUsername) { this.currentPlayUsername = currentPlayUsername; }
    public String getCurrentShape() { return currentShape; }
    public void setCurrentShape(String currentShape) { this.currentShape = currentShape; }
    public Integer getCurrentRank() { return currentRank; }
    public void setCurrentRank(Integer currentRank) { this.currentRank = currentRank; }
    public String getTrickPile() { return trickPile; }
    public void setTrickPile(String trickPile) { this.trickPile = trickPile; }
    public String getPassedUsernames() { return passedUsernames; }
    public void setPassedUsernames(String passedUsernames) { this.passedUsernames = passedUsernames; }
    public String getTrickLog() { return trickLog; }
    public void setTrickLog(String trickLog) { this.trickLog = trickLog; }
    public int getTrickLogNumber() { return trickLogNumber; }
    public void setTrickLogNumber(int trickLogNumber) { this.trickLogNumber = trickLogNumber; }
    public String getCurrentTurnUsername() { return currentTurnUsername; }
    public void setCurrentTurnUsername(String currentTurnUsername) { this.currentTurnUsername = currentTurnUsername; }
    public int getTrickNumber() { return trickNumber; }
    public void setTrickNumber(int trickNumber) { this.trickNumber = trickNumber; }
    public String getFirstOutUsername() { return firstOutUsername; }
    public void setFirstOutUsername(String firstOutUsername) { this.firstOutUsername = firstOutUsername; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
    public String getLeftByUsername() { return leftByUsername; }
    public void setLeftByUsername(String leftByUsername) { this.leftByUsername = leftByUsername; }
    public Instant getStartedDate() { return startedDate; }
    public void setStartedDate(Instant startedDate) { this.startedDate = startedDate; }
    public Instant getLastActionDate() { return lastActionDate; }
    public void setLastActionDate(Instant lastActionDate) { this.lastActionDate = lastActionDate; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<CardGamePlayer> getPlayers() { return players; }
    public void setPlayers(List<CardGamePlayer> players) { this.players = players; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CardGame other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
