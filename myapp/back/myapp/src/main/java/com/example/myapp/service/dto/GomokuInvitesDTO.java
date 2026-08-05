package com.example.myapp.service.dto;

import java.io.Serializable;
import java.util.List;

/** The current user's pending Gomoku invites, split by direction. */
public record GomokuInvitesDTO(List<GomokuGameDTO> incoming, List<GomokuGameDTO> outgoing) implements Serializable {
}
