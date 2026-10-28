package com.example.myapp.security;

import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findOneByLogin(username)
                .map(u -> User.withUsername(u.getLogin())
                        .password(u.getPassword())
                        .roles(u.getRole() != null && !u.getRole().isBlank() ? u.getRole() : Role.USER.name())
                        // An account disabled in User Management must not authenticate.
                        .disabled(Boolean.TRUE.equals(u.getDeleted()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
