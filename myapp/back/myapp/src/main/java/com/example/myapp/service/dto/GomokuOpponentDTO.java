package com.example.myapp.service.dto;

import java.io.Serializable;

/** A user eligible to be invited to a Gomoku match (a mutual follow of the current user). */
public record GomokuOpponentDTO(String login, String name, String avatar) implements Serializable {
}
