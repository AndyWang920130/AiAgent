package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Full state of a 70235 table <strong>as seen by one participant</strong>. Built per
 * recipient: {@link #myHand} holds only the requesting player's cards, and
 * {@link CardGamePlayerDTO} carries card counts rather than cards, so no response ever
 * discloses another player's hand.
 */
public record CardGameDTO(
    Long id,
    String status,
    String hostUsername,
    String hostName,
    /** Cards left undealt. A count, so clients can show the deck draining without seeing it. */
    int deckCount,
    int trickNumber,
    /** Shape of the play to beat: SINGLE, PAIR, FULL_HOUSE, QUAD_PLUS_ONE, or null on a lead. */
    String currentShape,
    /** The cards on the table (public — they have been played), or null on a fresh trick. */
    List<String> currentPlay,
    String currentPlayUsername,
    String currentPlayName,
    /** Points sitting in the trick pile, which the trick winner will collect. */
    int trickPoints,
    /**
     * Every play and pass in the trick identified by {@link #trickHistoryNumber}, oldest first,
     * so the UI can show a lead being raised around the table instead of just the play on top.
     */
    List<CardGameTrickEntryDTO> trickHistory,
    /**
     * The trick {@link #trickHistory} describes. Equal to {@link #trickNumber} while a trick is
     * in progress; one behind it just after a trick is scored, because the play that won the
     * trick is the last entry in that trick's history and stays attributed to it.
     */
    int trickHistoryNumber,
    String currentTurnUsername,
    List<CardGamePlayerDTO> players,
    /** The recipient's own cards, strongest first. Empty for a table that has not dealt yet. */
    List<String> myHand,
    /** The recipient's login, so the client can locate itself among the seats. */
    String myUsername,
    boolean host,
    /** True when the recipient may play or pass right now. */
    boolean myTurn,
    /** True when the host may deal: every invitee has answered and the seat count is legal. */
    boolean canStart,
    // There is deliberately no canRematch beside canStart. The first game needs a host to deal
    // it, because invitations can still be outstanding and somebody has to decide the table is
    // complete. The next game needs nobody: the room is already settled, so the last player to
    // ready up deals it. No button, no enabled state to report — the per-seat `ready` flags are
    // what the table draws instead.
    /** Emptied their hand first, collecting the point cards left in the other hands. */
    String firstOutUsername,
    String winnerUsername,
    String leftByUsername,
    Instant createdDate,
    // Clocks (seconds). turnTimeoutSeconds is the full per-turn budget; the *Remaining fields
    // are computed at response time and are null when not applicable to the current status.
    int turnTimeoutSeconds,
    Integer turnSecondsRemaining,
    Integer gameSecondsRemaining,
    Integer inviteSecondsRemaining,
    /**
     * How long the room stays open for another game. Its own field rather than a reuse of
     * {@link #inviteSecondsRemaining}: that one is the clock on invitations nobody has answered,
     * and a client showing "the invitation expires in" at a table that has already played a game
     * would be saying something untrue. Null outside {@code REMATCH}.
     */
    Integer rematchSecondsRemaining
) implements Serializable {
}
