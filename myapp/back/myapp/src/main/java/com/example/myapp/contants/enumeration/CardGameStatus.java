package com.example.myapp.contants.enumeration;

/**
 * Lifecycle of an online 70235 card game. A row starts as PENDING (the table being filled,
 * with invitations out), becomes ACTIVE when the host starts the deal, and reaches one of the
 * terminal states thereafter.
 *
 * <p>Unlike {@link GomokuGameStatus} there is no DECLINED or RESIGNED: a table can seat up to
 * five players, so one invitee declining does not end it (decline is tracked per seat, on
 * {@code CardGamePlayer.invitationStatus}) and there is nothing to resign to with more than
 * two players — a player who quits an active table leaves, giving ABANDONED.
 */
public enum CardGameStatus {
    PENDING,    // table created, waiting on invitees and the host's Start
    ACTIVE,     // dealt and being played
    FINISHED,   // deck empty and a player out, or the round cap reached — scored either way
    CANCELLED,  // host withdrew the table before it started
    EXPIRED,    // invitations not answered within the invite timeout
    TIMED_OUT,  // reserved for a table abandoned by inactivity rather than a deliberate leave
    ABANDONED   // a player left the active table, ending it for everyone
}
