package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Full state of an online Gomoku game as seen by one participant. {@code myColor} tells the
 * requesting client which side they play (1 = black, 2 = white), so the UI can decide whose
 * turn it is without extra bookkeeping.
 */
public record GomokuGameDTO(
    Long id,
    String blackUsername,
    String blackName,
    String blackAvatar,
    String whiteUsername,
    String whiteName,
    String whiteAvatar,
    String status,
    String board,
    int currentPlayer,
    Integer winner,
    Integer lastMoveRow,
    Integer lastMoveCol,
    int moveCount,
    int myColor,
    Instant createdDate,
    // Timeout clocks (seconds). moveTimeoutSeconds is the full per-move budget; the *Remaining
    // fields are computed at response time and are null when not applicable to the current status.
    int moveTimeoutSeconds,
    Integer moveSecondsRemaining,
    Integer gameSecondsRemaining,
    Integer inviteSecondsRemaining
) implements Serializable {
}
