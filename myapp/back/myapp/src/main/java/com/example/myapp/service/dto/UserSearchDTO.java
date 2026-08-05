package com.example.myapp.service.dto;

import java.io.Serializable;

/** A user matched by the header search box: enough to render a result and link to the profile. */
public record UserSearchDTO(String login, String name, String avatar) implements Serializable {
}
