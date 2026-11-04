package com.example.myapp.service.dto;

import java.io.Serializable;

/**
 * One seat at a 70235 table as every participant may see it.
 *
 * <p>There is deliberately <strong>no hand field on this record</strong>. A player's cards are
 * private, and omitting the field entirely means no code path exists that could serialise
 * them to another player — the protection is structural rather than a filter somebody could
 * forget to apply. The viewer's own cards travel separately, on
 * {@link CardGameDTO#myHand()}.
 */
public record CardGamePlayerDTO(
    String username,
    String name,
    String avatar,
    int seat,
    boolean cpu,
    /**
     * This player has handed their turns to the server. Reported to everyone at the table, not
     * just its owner: it is why a seat answers instantly, and unlike a hand it is not private.
     */
    boolean autoPlay,
    String invitationStatus,
    /** How many cards this player holds — a count, never the cards themselves. */
    int handCount,
    int score,
    /**
     * Declined the play currently on the table. Not out of the trick: a raise puts a new play
     * in front of them and the turn comes back round, so this clears whenever that happens.
     */
    boolean passed,
    /** Hand empty. Only reachable once the deck has run out. */
    boolean out
) implements Serializable {
}
