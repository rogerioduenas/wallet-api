package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

  @Mock
  private WalletRepository walletRepository;

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
}
