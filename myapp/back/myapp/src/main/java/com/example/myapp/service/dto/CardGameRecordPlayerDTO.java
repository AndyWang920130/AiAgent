package com.example.myapp.service.dto;

import java.io.Serializable;

/**
 * One seat's result in a recorded 70235 match, as the administrators' list shows it.
 *
 * <p>Like {@link CardGamePlayerDTO} there is no hand field — but here the reason is simply
 * that the match is over and nothing records the cards any more.
 */
public record CardGameRecordPlayerDTO(
    String username,
    /** Display name as it stands now, falling back to the login. CPU seats read "CPU 1". */
    String name,
    int seat,
    int score,
    boolean cpu,
    /**
     * This seat won. Derived from the record's winner rather than stored: one field decides it,
     * and a copy per seat could contradict it.
     */
    boolean winner
) implements Serializable {
}
