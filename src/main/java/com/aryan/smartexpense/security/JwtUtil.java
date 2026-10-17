package com.aryan.smartexpense.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key key;
    private final long jwtExpirationMs = 86400000; // 24 hours

    public JwtUtil(@Value("${JWT_SECRET:}") String configuredSecret) {
        byte[] secretBytes;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            secretBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        } else {
            try {
                secretBytes = java.util.Base64.getDecoder().decode(configuredSecret);
            } catch (IllegalArgumentException e) {
                secretBytes = configuredSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
