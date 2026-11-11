package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.response.BalanceResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

  @Mock
  private WalletRepository walletRepository;

  @Mock
  private Authentication authentication;

  private final Instant fixedInstant = Instant.parse("2026-10-07T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private WalletService walletService;

  @BeforeEach
  void setUp() {
    walletService = new WalletService(walletRepository, clock);
  }

  @Test
  @DisplayName("Should successfully create and save wallet for user")
  void givenUser_whenCreateWalletForUser_thenSaveAndReturnWallet() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User();
    ReflectionTestUtils.setField(user, "id", userId);

    Wallet expectedWallet = new Wallet(user, fixedInstant);

    given(walletRepository.save(any(Wallet.class))).willReturn(expectedWallet);

    // When
    Wallet createdWallet = walletService.createWalletForUser(user);

    // Then
    assertThat(createdWallet).isNotNull();
    assertThat(createdWallet.getUser()).isEqualTo(user);
    assertThat(createdWallet.getCreatedAt()).isEqualTo(fixedInstant);

    then(walletRepository).should().save(any(Wallet.class));
  }

  @Test
  @DisplayName("Should successfully return wallet balance when authenticated user has a valid wallet")
  void givenAuthenticatedUserWithWallet_whenGetBalance_thenReturnBalanceResponse() {
    // Given
    UUID customerId = UUID.randomUUID();
    mockAuthenticatedUser(customerId);

    Wallet wallet = new Wallet();
    BigDecimal expectedBalance = BigDecimal.valueOf(100.0);
    wallet.setBalance(expectedBalance);

    given(walletRepository.findByUserId(customerId)).willReturn(Optional.of(wallet));

    // When
    BalanceResponse response = walletService.getBalance();

    // Then
    assertThat(response).isNotNull();
    assertThat(response.customerId()).isEqualTo(customerId);
    assertThat(response.balance()).isEqualTo(expectedBalance);

    then(walletRepository).should().findByUserId(customerId);
  }

  @Test
  @DisplayName("Should throw WalletNotFoundException when authenticated user does not have an associated wallet")
  void givenAuthenticatedUserWithoutWallet_whenGetBalance_thenThrowWalletNotFoundException() {
    // Given
    UUID customerId = UUID.randomUUID();
    mockAuthenticatedUser(customerId);

    given(walletRepository.findByUserId(customerId))
        .willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> walletService.getBalance())
        .isInstanceOf(WalletNotFoundException.class)
        .hasMessage("Wallet not found");

  }

  @Test
  @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated in security context")
  void givenUnauthenticatedUser_whenGetBalance_thenThrowAuthenticationCredentialsNotFoundException() {
    // When & Then
    assertThatThrownBy(() -> walletService.getBalance())
    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessage("User is not authenticated");
  }

  private void mockAuthenticatedUser(UUID userId) {
    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getName()).willReturn(userId.toString());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
