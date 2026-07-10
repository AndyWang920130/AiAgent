package com.example.myapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailVerificationService {

    private static final long CODE_TTL_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, EmailCode> codes = new ConcurrentHashMap<>();
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:no-reply@myapp.local}}")
    private String from;

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCode(String email) {
        String normalizedEmail = normalize(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(normalizedEmail, new EmailCode(code, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        sendMail(normalizedEmail, code);
    }

    public boolean verify(String email, String code) {
        String normalizedEmail = normalize(email);
        EmailCode saved = codes.get(normalizedEmail);
        if (saved == null || saved.expiresAt().isBefore(Instant.now())) {
            codes.remove(normalizedEmail);
            return false;
        }
        boolean matched = saved.code().equals(code == null ? "" : code.trim());
        if (matched) {
            codes.remove(normalizedEmail);
        }
        return matched;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("MyApp registration verification code");
        message.setText("Your MyApp registration verification code is: " + code + "\n\nThis code expires in 5 minutes.");
        mailSender.send(message);
    }

    private record EmailCode(String code, Instant expiresAt) {}
}

