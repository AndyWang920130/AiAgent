package com.example.myapp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final InMemoryUserStore userStore;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userStore.findByUsername(username)
                .map(u -> User.withUsername(u.username())
                        .password(u.encodedPassword())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
