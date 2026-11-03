package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.TransactionRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import com.rogerio.wallet_api.mapper.TransactionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

  @Mock
  private WalletRepository walletRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private Authentication authentication;

  @Mock
  private TransactionMapper transactionMapper;

  private final Instant fixedInstant = Instant.parse("2026-10-15T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private UUID USER_ID;
  private Wallet WALLET;
  private DepositRequest VALID_REQUEST;

  private TransactionService transactionService;

  @BeforeEach
  void setUp() {
    transactionService = new TransactionService(
        walletRepository,
        transactionRepository,
        transactionMapper,
        clock
    );
    USER_ID = UUID.randomUUID();
    VALID_REQUEST = new DepositRequest(BigDecimal.valueOf(100.00));
    WALLET = new Wallet();
    WALLET.setBalance(BigDecimal.valueOf(00.0));
  }

  private void mockAuthenticatedUser(UUID userId) {
    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getName()).willReturn(userId.toString());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should successfully deposit amount and update wallet balance when valid request and authenticated user are provided")
  void givenValidRequestAndAuthenticatedUser_whenDeposit_thenSaveTransactionAndUpdateWallet() {
    // Given
    mockAuthenticatedUser(USER_ID);

    BigDecimal initialBalance = WALLET.getBalance();
    BigDecimal depositAmount = VALID_REQUEST.amount();
    BigDecimal expectedBalance = initialBalance.add(depositAmount);

    TransactionResponse expectedResponse = new TransactionResponse(UUID.randomUUID(), fixedInstant);

    given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));

    given(transactionRepository.save(any(Transaction.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(transactionMapper.toDto(any(Transaction.class))).willReturn(expectedResponse);

    // When
    TransactionResponse response = transactionService.deposit(VALID_REQUEST);

    // Then
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(expectedResponse);
    assertThat(WALLET.getBalance()).isEqualByComparingTo(expectedBalance);
  }

  @Test
  @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated in security context")
  void givenUnauthenticatedUser_whenDeposit_thenThrowAuthenticationCredentialsNotFoundException() {
    // When & Then
    assertThatThrownBy(() -> transactionService.deposit(VALID_REQUEST))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessage("User is not authenticated");

    then(walletRepository).shouldHaveNoInteractions();
    then(transactionRepository).shouldHaveNoInteractions();
    then(transactionMapper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should throw WalletNotFoundException when wallet does not exist for the authenticated user ID")
  void givenNonExistentWallet_whenDeposit_thenThrowWalletNotFoundException() {
    // Given
    mockAuthenticatedUser(USER_ID);
    given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> transactionService.deposit(VALID_REQUEST))
    .isInstanceOf(WalletNotFoundException.class)
        .hasMessage("Wallet not found");

    then(walletRepository).shouldHaveNoMoreInteractions();
    then(transactionRepository).shouldHaveNoInteractions();
    then(transactionMapper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should correctly accumulate amount to existing wallet balance when deposit is performed")
  void givenExistingBalance_whenDeposit_thenSumAmountToWalletBalance() {
    // Given
    mockAuthenticatedUser(USER_ID);
    WALLET.setBalance(BigDecimal.valueOf(50.00));

    BigDecimal expectedBalance = WALLET.getBalance().add(VALID_REQUEST.amount());

    given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));
    given(transactionRepository.save(any(Transaction.class))).willAnswer(inv -> inv.getArgument(0));
    given(transactionMapper.toDto(any(Transaction.class))).willReturn(mock(TransactionResponse.class));

    // When
    transactionService.deposit(VALID_REQUEST);

    // Then
    assertThat(WALLET.getBalance()).isEqualByComparingTo(expectedBalance);
  }
}




