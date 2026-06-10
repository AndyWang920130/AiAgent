package com.example.myapp.web;

import com.example.myapp.security.InMemoryUserStore;
import com.example.myapp.security.InMemoryUserStore.StoredUser;
import com.example.myapp.security.JwtUtil;
import com.example.myapp.web.vm.AuthResponse;
import com.example.myapp.web.vm.LoginRequest;
import com.example.myapp.web.vm.RegisterRequest;
import com.example.myapp.web.vm.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final InMemoryUserStore userStore;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
        StoredUser user = userStore.findByUsername(req.username()).orElseThrow();
        String token = jwtUtil.generate(req.username(), List.of("ROLE_USER"));
        return ResponseEntity.ok(new AuthResponse(token, new UserInfo(user.username(), user.name(), user.email())));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userStore.exists(req.username())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already taken"));
        }
        userStore.save(new StoredUser(
                req.username(), req.name(),
                req.email() != null ? req.email() : "",
                passwordEncoder.encode(req.password())));
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }
}
