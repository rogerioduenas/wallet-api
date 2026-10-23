package com.rogerio.wallet_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rogerio.wallet_api.domain.model.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @Mock
  private Clock clock;

  private TokenService tokenService;

  private static final String TEST_SECRET = "mySecretKeyForTestingPurposesOnlyNeedsToBeLongEnough32Bytes!";
  private static final long TEST_EXPIRATION = 3600000L;

  private final Instant fixedInstant = Instant.parse("2026-10-12T10:00:00Z");

  @BeforeEach
  void setUp() {
    tokenService = new TokenService(TEST_SECRET, TEST_EXPIRATION, clock);
  }

  @Test
  @DisplayName("Should generate a valid JWT token containing correct subject and role claim")
  void shouldGenerateValidTokenWithSubjectAndRole() {
    given(clock.instant()).willReturn(fixedInstant);

    String email = "email@email.com";
    String role = "ROLE_USER";

    // When
    String token = tokenService.generateToken(email, role);

    // Then
    assertThat(token).isNotBlank();

    SecretKey secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .clock(() -> java.util.Date.from(fixedInstant))
        .build()
        .parseSignedClaims(token)
        .getPayload();

    assertThat(claims.getSubject()).isEqualTo(email);
    assertThat(claims.get("role", String.class)).isEqualTo(role);
    assertThat(claims.getIssuedAt()).isEqualTo(java.util.Date.from(fixedInstant));
    assertThat(claims.getExpiration()).isEqualTo(java.util.Date.from(fixedInstant.plusMillis(TEST_EXPIRATION)));
  }

  @Test
  @DisplayName("Should return the correct expiration time in milliseconds")
  void shouldReturnCorrectExpirationInMs() {
    assertThat(tokenService.getExpirationInMs()).isEqualTo(TEST_EXPIRATION);
  }
}