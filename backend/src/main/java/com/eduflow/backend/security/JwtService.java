package com.eduflow.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("JWT_SECRET must be Base64 encoded");
        }
        if (decoded.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 decoded bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.key = Keys.hmacShaKeyFor(decoded);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String email) {
        return generateToken(email, 0);
    }

    public String generateToken(String email, long securityVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("eduflow")
                .subject(email)
                .claim("sv", securityVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer("eduflow")
                .build().parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null) {
            throw new JwtException("Missing required claims");
        }
        return claims.getSubject();
    }

    public long extractSecurityVersion(String token) {
        var claims = Jwts.parser().verifyWith(key).requireIssuer("eduflow").build().parseSignedClaims(token).getPayload();
        var version = claims.get("sv", Number.class);
        return version == null ? 0 : version.longValue();
    }
}
