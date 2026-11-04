package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.domain.repository.TransactionRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import com.rogerio.wallet_api.mapper.TransactionMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class TransactionService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final Clock clock;

  public TransactionService(WalletRepository walletRepository, TransactionRepository transactionRepository, TransactionMapper transactionMapper, Clock clock) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.transactionMapper = transactionMapper;
    this.clock = clock;
  }

  @Transactional
  public TransactionResponse deposit(DepositRequest request) {
    UUID userId = getCurrentUserIdFromSecurityContext();
    BigDecimal amount = request.amount();
    Wallet wallet = getWalletByUserId(userId);

    wallet.setBalance(wallet.getBalance().add(amount));

    return saveTransaction(wallet, amount, TransactionType.DEPOSIT);
  }

  @Transactional
  public TransactionResponse saveTransaction(Wallet wallet, BigDecimal amount, TransactionType type) {
    Transaction transaction = new Transaction(wallet, amount, type, clock.instant());
    transactionRepository.save(transaction);

    return transactionMapper.toDto(transaction);
  }

  private UUID getCurrentUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
    }
    return UUID.fromString(authentication.getName());
  }

  private Wallet getWalletByUserId(UUID userId) {
    return walletRepository.findByUserId(userId).orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
  }
}
