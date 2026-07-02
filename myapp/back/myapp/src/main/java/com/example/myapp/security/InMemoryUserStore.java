package com.example.myapp.security;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Deprecated
public class InMemoryUserStore {

    private final ConcurrentMap<String, StoredUser> users = new ConcurrentHashMap<>();

    public record StoredUser(String username, String name, String email, String encodedPassword) {}

    public void save(StoredUser user) {
        users.put(user.username(), user);
    }

    public Optional<StoredUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
