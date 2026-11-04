package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import com.rogerio.wallet_api.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest extends BaseWebTest {

  @MockitoBean
  private TransactionService transactionService;

  private final Instant fixedInstant = Instant.parse("2026-10-15T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private DepositRequest VALID_REQUEST;

  @BeforeEach
  void setUp() {
    VALID_REQUEST = new DepositRequest(BigDecimal.valueOf(100.00));
  }

  @Test
  @DisplayName("Should return 201 Created with transaction response and location header when deposit request is valid")
  void givenValidDepositRequest_whenDeposit_thenReturnsCreatedWithTransactionResponse() throws Exception {
    // Given
    TransactionResponse response = new TransactionResponse(UUID.randomUUID(), clock.instant());

    given(transactionService.deposit(any(DepositRequest.class))).willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/wallets/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST))
            .with(user("email@email.com").roles("USER")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/v1/transactions/")))
        .andExpect(jsonPath("$.id").value(response.id().toString()));
  }

  @ParameterizedTest
  @DisplayName("Should return 400 Bad Request when deposit request has null, zero or negative amount")
  @CsvSource({
      "null",
      "0.00",
      "-0.01",
      "-10.00"
  })
  void givenInvalidAmount_whenDeposit_thenReturnsBadRequest(String amountStr) throws Exception {
    // Given
    BigDecimal amount = "null".equals(amountStr) ? null : new BigDecimal(amountStr);
    DepositRequest invalidRequest = new DepositRequest(amount);

    // When & Then
    mockMvc.perform(post("/api/v1/wallets/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .with(user("email@email.com").roles("USER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when user is not authenticated")
  void givenUnauthenticatedUser_whenDeposit_thenReturnsUnauthorized() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/v1/wallets/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 404 Not Found when wallet does not exist for the authenticated user")
  void givenNonExistentWallet_whenDeposit_thenReturnsNotFound() throws Exception {
    // Given
    given(transactionService.deposit(any(DepositRequest.class)))
        .willThrow(new WalletNotFoundException("Wallet not found"));

    // When & Then
    mockMvc.perform(post("/api/v1/wallets/deposit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST))
            .with(user("email@email.com").roles("USER")))
        .andExpect(status().isNotFound());
  }
}
