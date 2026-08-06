package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.GomokuGameStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * An online two-player Gomoku game. A single row models both the invitation and the match:
 * it starts PENDING (the invite), becomes ACTIVE on accept, then terminal. Black is the
 * inviter and always moves first. The board is the authoritative game state (server-side).
 */
@Entity
@Table(
    name = "twsny_gomoku_game",
    indexes = {
        @Index(name = "idx_twsny_gomoku_game_black", columnList = "black_username"),
        @Index(name = "idx_twsny_gomoku_game_white", columnList = "white_username"),
        @Index(name = "idx_twsny_gomoku_game_status", columnList = "status")
    }
)
public class GomokuGame extends AbstractAuditingEntity {

    /** 15x15 board; a fresh board is 225 '0' characters. */
    public static final int BOARD_SIZE = 15;
    public static final String EMPTY_BOARD = "0".repeat(BOARD_SIZE * BOARD_SIZE);

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Size(max = 100)
    @Column(name = "black_username", nullable = false, length = 100)
    private String blackUsername;

    @NotNull @Size(max = 100)
    @Column(name = "white_username", nullable = false, length = 100)
    private String whiteUsername;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GomokuGameStatus status;

    /** Row-major 15x15 board of '0' (empty), '1' (black), '2' (white). */
    @NotNull @Size(max = 225)
    @Column(name = "board", nullable = false, length = 225)
    private String board = EMPTY_BOARD;

    /** Whose turn it is while ACTIVE: 1 = black, 2 = white. */
    @Column(name = "current_player", nullable = false)
    private int currentPlayer = 1;

    /** Winner once terminal: 1 = black, 2 = white, 0 = draw; null while unresolved. */
    @Column(name = "winner")
    private Integer winner;

    /** Participant who left the match screen for the lobby. Used to close the opponent's UI too. */
    @Column(name = "left_by_username", length = 100)
    private String leftByUsername;

    @Column(name = "last_move_row")
    private Integer lastMoveRow;

    @Column(name = "last_move_col")
    private Integer lastMoveCol;

    @Column(name = "move_count", nullable = false)
    private int moveCount = 0;

    /** When the game became ACTIVE (drives the whole-round timeout). Null while PENDING. */
    @Column(name = "started_date")
    private Instant startedDate;

    /** When the current turn's clock started — set on accept and on every move (drives the per-move timeout). */
    @Column(name = "last_move_date")
    private Instant lastMoveDate;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBlackUsername() { return blackUsername; }
    public void setBlackUsername(String blackUsername) { this.blackUsername = blackUsername; }
    public String getWhiteUsername() { return whiteUsername; }
    public void setWhiteUsername(String whiteUsername) { this.whiteUsername = whiteUsername; }
    public GomokuGameStatus getStatus() { return status; }
    public void setStatus(GomokuGameStatus status) { this.status = status; }
    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }
    public int getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(int currentPlayer) { this.currentPlayer = currentPlayer; }
    public Integer getWinner() { return winner; }
    public void setWinner(Integer winner) { this.winner = winner; }
    public String getLeftByUsername() { return leftByUsername; }
    public void setLeftByUsername(String leftByUsername) { this.leftByUsername = leftByUsername; }
    public Integer getLastMoveRow() { return lastMoveRow; }
    public void setLastMoveRow(Integer lastMoveRow) { this.lastMoveRow = lastMoveRow; }
    public Integer getLastMoveCol() { return lastMoveCol; }
    public void setLastMoveCol(Integer lastMoveCol) { this.lastMoveCol = lastMoveCol; }
    public int getMoveCount() { return moveCount; }
    public void setMoveCount(int moveCount) { this.moveCount = moveCount; }
    public Instant getStartedDate() { return startedDate; }
    public void setStartedDate(Instant startedDate) { this.startedDate = startedDate; }
    public Instant getLastMoveDate() { return lastMoveDate; }
    public void setLastMoveDate(Instant lastMoveDate) { this.lastMoveDate = lastMoveDate; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GomokuGame other && getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
