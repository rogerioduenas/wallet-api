package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.request.TransferRequest;
import com.rogerio.wallet_api.dto.request.WithdrawRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import com.rogerio.wallet_api.exception.*;
import com.rogerio.wallet_api.handler.ErrorTypes;
import com.rogerio.wallet_api.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
      // When & Then
      mockMvc.perform(post("/api/v1/wallets/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isUnprocessableContent());
    }
  }

  @Nested
  @DisplayName("Transfer")
  class Transfer {

    @Test
    @DisplayName("Should return 201 Created with transaction response and location header when transfer is successful")
    void givenValidTransferRequest_whenTransfer_thenReturnsCreatedWithResponse() throws Exception {
      // Given
      TransferRequest request = new TransferRequest(UUID.randomUUID(), BigDecimal.valueOf(100.00));
      TransactionResponse response = new TransactionResponse(UUID.randomUUID(), clock.instant());

      given(transactionService.transfer(any(TransferRequest.class))).willReturn(response);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", containsString("/api/v1/transactions/")))
          .andExpect(jsonPath("$.id").value(response.id().toString()))
          .andExpect(jsonPath("$.createdAt").value(response.createdAt().toString()));
    }

    @ParameterizedTest
    @DisplayName("Should return 400 Bad Request when amount is negative or zero")
    @CsvSource({
        "null",
        "0.00",
        "-0.01",
        "-10.00"
    })
    void givenInvalidAmount_whenTransfer_thenReturnsBadRequest(String amountStr) throws Exception {
      // Given
      BigDecimal amount = "null".equals(amountStr) ? null : new BigDecimal(amountStr);
      TransferRequest request = new TransferRequest(UUID.randomUUID(), amount);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email.com").roles("USER")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type")
              .value(ErrorTypes.INVALID_PARAMS.toString()));

      then(transactionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @DisplayName("Should return 400 Bad Request when recipientId is blank or invalid format")
    @CsvSource({
        "''",
        "invalid-uuid"
    })
    void givenInvalidRecipient_whenTransfer_thenReturnsBadRequest(String recipientIdString) throws Exception {
      String requestJson = String.format("{\"recipientId\": \"%s\", \"amount\": 50.00}", recipientIdString);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson)
              .with(user("email.com").roles("USER")))
          .andExpect(status().isBadRequest());

      then(transactionService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return appropriate error response when service throws SelfTransferException")
    void givenSameSenderAndRecipient_whenTransfer_thenReturnsErrorResponse() throws Exception {
      // Given
      TransferRequest request = new TransferRequest(UUID.randomUUID(), BigDecimal.valueOf(100.00));

      given(transactionService.transfer(any(TransferRequest.class)))
          .willThrow(SelfTransferException.class);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email.com").roles("USER")))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.type")
              .value(ErrorTypes.SELF_TRANSFER_NOT_ALLOWED.toString()));
    }

    @Test
    @DisplayName("Should return appropriate error response when sender has insufficient balance")
    void givenInsufficientBalance_whenTransfer_thenReturnsErrorResponse() throws Exception {
      // Given
      TransferRequest request = new TransferRequest(UUID.randomUUID(), BigDecimal.valueOf(100.00));

      given(transactionService.transfer(any(TransferRequest.class)))
          .willThrow(InsufficientBalanceException.class);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email.com").roles("USER")))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.type")
              .value(ErrorTypes.INSUFFICIENT_BALANCE.toString()));
    }

    @Test
    @DisplayName("Should return appropriate error response when recipient is not found")
    void givenNonRecipient_whenTransfer_thenReturnsErrorResponse() throws Exception {
      // Given
      TransferRequest request = new TransferRequest(UUID.randomUUID(), BigDecimal.valueOf(100.00));

      given(transactionService.transfer(any(TransferRequest.class)))
          .willThrow(UserNotFoundException.class);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email.com").roles("USER")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.type")
              .value(ErrorTypes.NOT_FOUND.toString()));
    }

    @Test
    @DisplayName("Should return appropriate error response when recipient is inactive")
    void givenInactiveRecipient_whenTransfer_thenReturnsErrorResponse() throws Exception {
      // Given
      TransferRequest request = new TransferRequest(UUID.randomUUID(), BigDecimal.valueOf(100.00));

      given(transactionService.transfer(any(TransferRequest.class))).willThrow(UserBlockedException.class);

      // When & Then
      mockMvc.perform(post("/api/v1/wallets/transfer")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .with(user("email.com").roles("USER")))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.type")
              .value(ErrorTypes.FORBIDDEN.toString()));

    }
  }

  @Nested
  @DisplayName("Statement")
  class Statement {

    @Test
    @DisplayName("Should return 200 OK with paginated statement when request is valid")
    void givenValidParameters_whenGetStatement_thenReturnsPagedStatement() throws Exception {
      // Given
      Pageable pageable = PageRequest.of(0, 3);
      TransactionStatementResponse statementResponse = new TransactionStatementResponse(
          UUID.randomUUID(),
          BigDecimal.valueOf(150.00),
          TransactionType.DEPOSIT,
          null,
          Instant.now()
      );
      Page<TransactionStatementResponse> pagedResponse = new PageImpl<>(List.of(statementResponse), pageable, 1);

      given(transactionService.getCustomerStatement(any(), any(), any(), any(Pageable.class)))
          .willReturn(pagedResponse);

      // When & Then
      mockMvc.perform(get("/api/v1/wallets/statement")
              .param("page", "0")
              .param("size", "3")
              .param("type", "DEPOSIT")
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].id").value(statementResponse.id().toString()))
          .andExpect(jsonPath("$.content[0].amount").value(statementResponse.amount().doubleValue()))
          .andExpect(jsonPath("$.content[0].type").value(statementResponse.type().toString()))
          .andExpect(jsonPath("$.totalElements").value(1));

      then(transactionService).should().getCustomerStatement(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return 200 OK with empty page when statement has no transactions")
    void givenNoTransactions_whenGetStatement_thenReturnsEmptyPagedStatement() throws Exception {
      // Given
      Pageable pageable = PageRequest.of(0, 3);
      Page<TransactionStatementResponse> emptyPagedResponse = new PageImpl<>(List.of(), pageable, 0);

      given(transactionService.getCustomerStatement(any(), any(), any(), any(Pageable.class)))
          .willReturn(emptyPagedResponse);

      // When & Then
      mockMvc.perform(get("/api/v1/wallets/statement")
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.totalElements").value(0));

      then(transactionService).should().getCustomerStatement(any(), any(), any(), any(Pageable.class));
    }

    @ParameterizedTest
    @DisplayName("Should return 400 Bad Request when parameters have invalid format")
    @CsvSource({
        "startDate, invalid-date-format",
        "endDate,   invalid-date-format",
        "type,      invalid-transaction-type"
    })
    void givenInvalidParameters_whenGetStatement_thenReturnsBadRequest(String paramName, String paramValue) throws Exception {
      // When & Then
      mockMvc.perform(get("/api/v1/wallets/statement")
              .param(paramName, paramValue)
              .with(user("email@email.com").roles("USER")))
          .andExpect(status().isBadRequest());

      then(transactionService).shouldHaveNoInteractions();
    }
  }
}
