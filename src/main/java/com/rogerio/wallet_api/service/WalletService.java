package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.response.BalanceResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final Clock clock;

  public WalletService(WalletRepository walletRepository, Clock clock) {
    this.walletRepository = walletRepository;
    this.clock = clock;
  }

  @Transactional
  public Wallet createWalletForUser(User user) {
    Wallet wallet = new Wallet(user, clock.instant());
    return walletRepository.save(wallet);
  }

  public BalanceResponse getBalance() {
    UUID customerId = getCurrentUserIdFromSecurityContext();
    Wallet wallet = walletRepository.findByUserId(customerId)
        .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

    return new BalanceResponse(customerId, wallet.getBalance());
  }

  private UUID getCurrentUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
    }
    return UUID.fromString(authentication.getName());
  }
}
