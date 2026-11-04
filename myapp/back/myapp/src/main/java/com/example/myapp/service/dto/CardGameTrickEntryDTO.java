package com.example.myapp.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * One action in the current trick, in the order it happened: a play or a pass, attributed to a
 * seat.
 *
 * <p>This exists so a client can show a lead being raised — 8, then 9, then 10 — rather than
 * only the play sitting on top. Several seats often act inside one request, because every CPU
 * between one human turn and the next moves while the table is advanced, so the intermediate
 * plays are invisible without a record of them.
 *
 * <p>Discloses nothing hidden: these cards have all been played face up.
 */
public record CardGameTrickEntryDTO(
    String username,
    String name,
    int seat,
    /** The cards played, or empty for a pass. */
    List<String> cards,
    boolean passed,
    /** Shape of this play (null for a pass), so the UI can label it without re-deriving it. */
    String shape
) implements Serializable {
}
