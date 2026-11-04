package com.example.myapp.service.dto;

import java.io.Serializable;

/**
 * A user the caller may invite to a 70235 table — one of their followers. The one-way follow
 * rule applies: anyone who follows the caller can be invited, whether or not the caller
 * follows them back.
 */
public record CardGameOpponentDTO(String username, String name, String avatar) implements Serializable {
}
