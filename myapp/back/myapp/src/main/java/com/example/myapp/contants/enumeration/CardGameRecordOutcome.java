package com.example.myapp.contants.enumeration;

/**
 * How a recorded 70235 match ended.
 *
 * <p>Deliberately not {@link CardGameStatus}. That enum describes the room, where REMATCH and
 * FINISHED are the same thing as far as a played match is concerned — the first is the result
 * standing while the players decide on another game, the second is that decision lapsing. A
 * match record is written once, when the match ends, so it needs the two outcomes a match can
 * actually have rather than the eight states a room moves through.
 */
public enum CardGameRecordOutcome {
    /** Played to the end and scored: every seat has a final score and there is a winner. */
    SCORED,

    /**
     * A player walked out mid-game. The scores are as they stood at that moment and there is no
     * winner: the end-of-game scoring never ran, so no leftover-point bonus was awarded.
     */
    ABANDONED
}
