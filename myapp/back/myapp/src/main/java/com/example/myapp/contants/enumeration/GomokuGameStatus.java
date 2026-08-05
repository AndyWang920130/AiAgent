package com.example.myapp.contants.enumeration;

/**
 * Lifecycle of an online Gomoku game. A row starts as PENDING (the invitation), becomes
 * ACTIVE once the invitee accepts, and reaches one of the terminal states thereafter.
 */
public enum GomokuGameStatus {
    PENDING,    // invite sent, waiting for the invitee to accept
    ACTIVE,     // accepted, currently being played
    FINISHED,   // ended by a five-in-a-row win or a full-board / round-timeout draw
    DECLINED,   // invitee declined the invite
    CANCELLED,  // inviter cancelled the pending invite
    RESIGNED,   // a participant resigned an active game
    EXPIRED,    // invite not accepted within the invite timeout
    TIMED_OUT   // player to move did not play within the move timeout (they forfeit)
}
