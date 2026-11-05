package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.request.WithdrawRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.exception.InsufficientBalanceException;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import com.rogerio.wallet_api.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

  @Nested
  @DisplayName("Deposit")
  class Deposit {

    @Test
    @DisplayName("Should return 201 Created with transaction response and location header when deposit request is valid")
    void givenValidDepositRequest_whenDeposit_thenReturnsCreatedWithTransactionResponse() throws Exception {
      // Given
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));
      TransactionResponse response = new TransactionResponse(UUID.randomUUID(), clock.instant());

      given(transactionService.deposit(any(DepositRequest.class))).willReturn(response);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/deposit")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
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
      // Given
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/deposit")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 Not Found when wallet does not exist for the authenticated user")
    void givenNonExistentWallet_whenDeposit_thenReturnsNotFound() throws Exception {
      // Given
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));

      given(transactionService.deposit(any(DepositRequest.class)))
          .willThrow(new WalletNotFoundException("Wallet not found"));

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/deposit")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Withdraw")
  class Withdraw {

    @Test
    @DisplayName("Should return 201 Created with transaction response and location header when withdraw request is valid")
    void givenValidWithdrawRequest_whenWithdraw_thenReturnsCreatedWithTransactionResponse() throws Exception {
      // Given
      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(100.00));
      TransactionResponse response = new TransactionResponse(UUID.randomUUID(), clock.instant());

      given(transactionService.withdraw(any(WithdrawRequest.class))).willReturn(response);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", containsString("/api/v1/transactions/")))
          .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @ParameterizedTest
    @DisplayName("Should return 400 Bad Request when withdraw request has null, zero or negative amount")
    @CsvSource({
        "null",
        "0.00",
        "-0.01",
        "-10.00"
    })
    void givenInvalidAmount_whenWithdraw_thenReturnsBadRequest(String amountStr) throws Exception {
      // Given
      BigDecimal amount = "null".equals(amountStr) ? null : new BigDecimal(amountStr);
      WithdrawRequest invalidRequest = new WithdrawRequest(amount);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when user is not authenticated during withdraw")
    void givenUnauthenticatedUser_whenWithdraw_thenReturnsUnauthorized() throws Exception {
      // Given
      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(100.00));

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 422 Unprocessable Content when wallet has insufficient balance")
    void givenInsufficientBalanceOrMissingWallet_whenWithdraw_thenReturnsErrorStatus() throws Exception {
      // Given
      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(100.00));

      given(transactionService.withdraw(any(WithdrawRequest.class))).willThrow(
          new InsufficientBalanceException("Insufficient Balance")
      );
      // When
      mockMvc.perform(post("/api/v1/wallets/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isUnprocessableContent());
          // Then
    }
  }
}
