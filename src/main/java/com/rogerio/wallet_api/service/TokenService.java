package com.rogerio.wallet_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

  private final SecretKey secretKey;
  @Getter
  private final long expirationInMs;
  private final Clock clock;

  public TokenService(
      @Value("${api.security.jwt.secret}") String secret,
      @Value("${api.security.jwt.expiration-ms}") long expirationInMs,
      Clock clock) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationInMs = expirationInMs;
    this.clock = clock;
  }

  public String generateToken(String email, String role) {
    Instant now = clock.instant();
    Instant expiryDate = now.plusMillis(expirationInMs);

    return Jwts.builder()
        .subject(email)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiryDate))
        .signWith(secretKey)
        .compact();
  }

  public boolean isTokenValid(String token) {
    try {
      Claims claims = parseClaims(token);
      return !claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public String extractUserEmail(String token) {
    Claims claims = parseClaims(token);
    return claims.getSubject();
  }

  public String extractRole(String token) {
    Claims claims = parseClaims(token);
    return claims.get("role", String.class);
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
