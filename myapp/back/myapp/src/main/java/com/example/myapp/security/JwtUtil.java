package com.example.myapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static final String CLAIM_ROLES = "roles";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /** Generate a token that embeds the user's roles so the filter needs no DB lookup. */
    public String generate(String username, Collection<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLES, String.join(",", roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        String roles = claims(token).get(CLAIM_ROLES, String.class);
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.stream(roles.split(","))
                .filter(r -> !r.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }
}
