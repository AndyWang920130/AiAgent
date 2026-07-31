package com.example.myapp.web.rest;

import com.example.myapp.contants.enumeration.AchievementType;
import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.security.JwtUtil;
import com.example.myapp.service.AchievementService;
import com.example.myapp.service.EmailVerificationService;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.vm.AuthResponse;
import com.example.myapp.web.rest.vm.ChangePasswordRequest;
import com.example.myapp.web.rest.vm.ForgotPasswordRequest;
import com.example.myapp.web.rest.vm.LoginRequest;
import com.example.myapp.web.rest.vm.ProfileResponse;
import com.example.myapp.web.rest.vm.RegisterRequest;
import com.example.myapp.web.rest.vm.ResetPasswordRequest;
import com.example.myapp.web.rest.vm.UpdateProfileRequest;
import com.example.myapp.web.rest.vm.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final AchievementService achievementService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
        User user = userRepository.findOneByLogin(req.username()).orElseThrow();
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        String token = jwtUtil.generate(req.username(), List.of("ROLE_" + role.name()));
        return ResponseEntity.ok(new AuthResponse(token, new UserInfo(user.getLogin(), user.getRealName(), user.getEmail(), role.name())));
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
        achievementService.award(req.username(), AchievementType.REGISTRATION);
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    /**
     * Change the current (authenticated) user's password. The old password is verified before
     * the new one is stored; a wrong current password is rejected with 400 rather than silently
     * ignored. Requires authentication — see SecurityConfig, where this path is exempted from the
     * permitAll rule that covers the rest of /api/v1/auth/**.
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        String username = SecurityUtil.getCurrentUsername();
        User user = userRepository.findOneByLogin(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Return the current (authenticated) user's editable profile. The login and join date are
     * read-only; the remaining fields map to editable columns (name -> realName, bio -> description).
     * Requires authentication — see SecurityConfig, where /api/v1/auth/profile is exempted from the
     * permitAll rule that covers the rest of /api/v1/auth/**.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String username = SecurityUtil.getCurrentUsername();
        User user = userRepository.findOneByLogin(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        return ResponseEntity.ok(toProfile(user));
    }

    /**
     * Update the current user's own profile. Only name, email and bio are accepted (username is
     * immutable). The email must not already belong to a different account.
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        String username = SecurityUtil.getCurrentUsername();
        User user = userRepository.findOneByLogin(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        User emailOwner = userRepository.findOneByEmailIgnoreCase(req.email()).orElse(null);
        if (emailOwner != null && !emailOwner.getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        user.setRealName(req.name());
        user.setNickName(req.name());
        user.setEmail(req.email());
        user.setDescription(req.bio());
        userRepository.save(user);
        return ResponseEntity.ok(toProfile(user));
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(
                user.getLogin(),
                user.getRealName(),
                user.getEmail(),
                user.getDescription(),
                user.getCreatedDate());
    }

    /**
     * Step 1 of the forgot-password flow: email a reset code to the address. If no account uses
     * the email, the caller is told so explicitly (per product requirement) — note this allows
     * email enumeration. A mail-transport failure is surfaced as 400.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        if (userRepository.findOneByEmailIgnoreCase(req.email()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email not found. Please check and try again."));
        }
        try {
            emailVerificationService.sendResetCode(req.email());
        } catch (MailException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to send reset code"));
        }
        return ResponseEntity.ok(Map.of("message", "A reset code has been sent to your email"));
    }

    /**
     * Step 2 of the forgot-password flow: verify the emailed code and set the new password.
     * The code is single-use (consumed on success by the verification service). An invalid or
     * expired code is rejected with 400.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        if (!emailVerificationService.verify(req.email(), req.code())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired reset code"));
        }
        User user = userRepository.findOneByEmailIgnoreCase(req.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired reset code"));
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
