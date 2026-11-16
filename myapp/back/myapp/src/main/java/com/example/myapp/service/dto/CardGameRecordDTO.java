package com.example.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * One finished 70235 match for the administrators' match record list.
 *
 * <p>Carries both ends of the match — {@code startedDate} and {@code endedDate} — and a score
 * for every seat. Duration is deliberately absent: it is the difference between two fields
 * already here, and the client formats it.
 */
public record CardGameRecordDTO(
    Long id,
    /** The room the match was played in. Several records can share this. */
    Long gameId,
    String hostUsername,
    /** The creator's display name as it stands now, falling back to the login. */
    String hostName,
    Instant startedDate,
    Instant endedDate,
    /** SCORED or ABANDONED, from {@code CardGameRecordOutcome}. */
    String outcome,
    /** Null on an ABANDONED match, which never reached scoring. */
    String winnerUsername,
    String winnerName,
    String firstOutUsername,
    /** Who walked out, on an ABANDONED match. Null otherwise. */
    String leftByUsername,
    String leftByName,
    int trickCount,
    int playerCount,
    /** Every seat, in seat order. */
    List<CardGameRecordPlayerDTO> players
) implements Serializable {
}
