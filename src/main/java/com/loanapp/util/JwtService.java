package com.loanapp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @PostConstruct
    public void init() {
        System.out.println("JWT secret loaded: " + secret);
    }
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry-ms:900000}")      // default 15 min
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")  // default 7 days
    private long refreshTokenExpiryMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Long useraccountId) {
        return buildToken(username, useraccountId, accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(String username, Long useraccountId) {
        return buildToken(username, useraccountId, refreshTokenExpiryMs, "refresh");
    }

    private String buildToken(String username, Long useraccountId, long expiryMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .claim("username", username)
                .claim("userid", useraccountId)
                .claim("type", type)
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and validates the token in one step. Throws JwtException subtypes
     * (ExpiredJwtException, MalformedJwtException, SignatureException,
     * UnsupportedJwtException, IllegalArgumentException) on any invalid token —
     * the filter must catch these and respond 401, not let them propagate as 500s.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            String extractedUsername = claims.get("username", String.class);
            return extractedUsername.equals(username) && !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }
}
