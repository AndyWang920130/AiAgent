package com.example.myapp.config;

import com.example.myapp.security.InMemoryUserStore;
import com.example.myapp.security.InMemoryUserStore.StoredUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final InMemoryUserStore userStore;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    void init() {
        userStore.save(new StoredUser("admin", "Administrator", "admin@example.com", passwordEncoder.encode("admin")));
        userStore.save(new StoredUser("user", "Demo User", "user@example.com", passwordEncoder.encode("password")));
    }
}
