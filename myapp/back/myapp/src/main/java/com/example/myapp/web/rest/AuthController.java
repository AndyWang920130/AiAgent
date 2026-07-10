package com.example.myapp.web.rest;

import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.security.JwtUtil;
import com.example.myapp.service.EmailVerificationService;
import com.example.myapp.web.rest.vm.AuthResponse;
import com.example.myapp.web.rest.vm.LoginRequest;
import com.example.myapp.web.rest.vm.RegisterRequest;
import com.example.myapp.web.rest.vm.UserInfo;
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
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
        User user = userRepository.findOneByLogin(req.username()).orElseThrow();
        String token = jwtUtil.generate(req.username(), List.of("ROLE_USER"));
        return ResponseEntity.ok(new AuthResponse(token, new UserInfo(user.getLogin(), user.getRealName(), user.getEmail())));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByLogin(req.username())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already taken"));
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        if (!emailVerificationService.verify(req.email(), req.emailCode())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email verification code"));
        }
        userRepository.save(new User()
                .login(req.username())
                .realName(req.name())
                .nickName(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .deleted(false));
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }
}
