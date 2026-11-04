package com.example.myapp.contants.enumeration;

/**
 * Where one seat stands in a 70235 table's invitation phase. Tracked per seat rather than on
 * the game, because a 2-5 player table survives one invitee declining.
 *
 * <p>The host and every CPU seat are ACCEPTED from the moment the table is created — only
 * invited humans are ever PENDING.
 */
public enum CardGameInvitationStatus {
    PENDING,   // invited, has not answered yet — blocks the host's Start
    ACCEPTED,  // in the game
    DECLINED   // turned the invite down; the seat is dropped when the host starts
}
