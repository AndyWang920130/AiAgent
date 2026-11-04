package com.example.myapp.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * The caller's pending 70235 tables, split by direction: {@code incoming} are invitations
 * awaiting their answer, {@code outgoing} are tables they host that have not started yet.
 */
public record CardGameInvitesDTO(List<CardGameDTO> incoming, List<CardGameDTO> outgoing) implements Serializable {
}
