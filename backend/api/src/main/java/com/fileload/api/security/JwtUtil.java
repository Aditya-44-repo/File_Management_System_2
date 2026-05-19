package com.fileload.api.security;
//Class Purpose: Manages the creation and verification of JWT tokens for user authentication.



import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    /// Constructor that initializes the secret key and token expiration time from application properties
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //Creates a basic token with default version (0) and "USER" role, recursion
    public String generateToken(String username) {
        return generateToken(username, 0, "USER");
    }

    // Method overloading of returned above method
    //Creates a token with custom version and role claims
    public String generateToken(String username, int tokenVersion, String role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationMs, ChronoUnit.MILLIS);

        // token is generated using the following data
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "tokenVersion", tokenVersion,
                        "role", role
                ))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

//    Extracts the username from a valid token
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    //Checks if a token is valid and not expired
    public boolean isTokenValid(String token) {
        try {
            //function used for extracting
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //Retrieves the tokenVersion claim from the token
    public int extractTokenVersion(String token) {
        Object claimValue = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("tokenVersion");

        if (claimValue instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}

