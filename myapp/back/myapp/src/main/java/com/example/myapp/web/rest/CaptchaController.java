package com.example.myapp.web.rest;

import com.example.myapp.service.EmailVerificationService;
import com.example.myapp.web.rest.vm.EmailCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/code")
@RequiredArgsConstructor
public class CaptchaController {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/image")
    public ResponseEntity<String> captcha() {
        String code = generateCode(4);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // Returns "uuid_CODE" — the front-end splits on '_' to get both parts
        return ResponseEntity.ok(uuid + "_" + code);
    }

    @PostMapping("/email")
    public ResponseEntity<?> emailCode(@Valid @RequestBody EmailCodeRequest request) {
        try {
            emailVerificationService.sendCode(request.email());
            return ResponseEntity.ok(java.util.Map.of("message", "Verification code sent"));
        } catch (MailException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Failed to send verification code"));
        }
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
