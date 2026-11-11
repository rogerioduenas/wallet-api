package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;

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
}
