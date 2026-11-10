package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.model.enums.Status;
import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.domain.repository.TransactionRepository;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.request.TransferRequest;
import com.rogerio.wallet_api.dto.request.WithdrawRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.exception.*;
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
  private final UserRepository userRepository;
  private final TransactionMapper transactionMapper;
  private final Clock clock;

  public TransactionService(WalletRepository walletRepository,
                            TransactionRepository transactionRepository,
                            UserRepository userRepository,
                            TransactionMapper transactionMapper,
                            Clock clock) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.transactionMapper = transactionMapper;
    this.clock = clock;
  }

  @Transactional
  public TransactionResponse deposit(DepositRequest request) {
    UUID userId = getCurrentUserIdFromSecurityContext();
    BigDecimal amount = request.amount();
    Wallet wallet = getWalletByUserId(userId);

    wallet.setBalance(wallet.getBalance().add(amount));

    return saveTransaction(wallet, amount, TransactionType.DEPOSIT, null);
  }

  @Transactional
  public TransactionResponse withdraw(WithdrawRequest request) {
    UUID userId = getCurrentUserIdFromSecurityContext();
    BigDecimal amount = request.amount();
    Wallet wallet = getWalletByUserId(userId);

    ensureSufficientBalance(wallet, amount);

    wallet.setBalance(wallet.getBalance().subtract(amount));

    return saveTransaction(wallet, amount, TransactionType.WITHDRAW, null);
  }

  @Transactional
  public TransactionResponse transfer(TransferRequest request) {
    UUID senderId = getCurrentUserIdFromSecurityContext();
    BigDecimal amount = request.amount();

    if (senderId.equals(request.recipientId())) {
      throw new SelfTransferException("You can't transfer funds to yourself");
    }

    Wallet senderWallet = getWalletByUserId(senderId);
    Wallet recipientWallet = getWalletFromActiveUser(request.recipientId());

    ensureSufficientBalance(senderWallet, amount);

    senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
    walletRepository.save(senderWallet);
    recipientWallet.setBalance(recipientWallet.getBalance().add(amount));
    walletRepository.save(recipientWallet);

    return saveTransaction(senderWallet, amount, TransactionType.TRANSFER, recipientWallet);
  }

  public TransactionResponse saveTransaction(
      Wallet wallet,
      BigDecimal amount,
      TransactionType type,
      Wallet recipientWallet) {
    Transaction transaction = new Transaction(
        wallet,
        amount,
        type,
        recipientWallet,
        clock.instant());
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

  private void ensureSufficientBalance(Wallet wallet, BigDecimal amount) {
    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new InsufficientBalanceException("Insufficient balance");
    }
  }

  private Wallet getWalletFromActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    if (user.getStatus() != Status.ACTIVE) {
      throw new UserBlockedException("User is not active");
    }

    return getWalletByUserId(userId);
  }

  private Wallet getWalletByUserId(UUID userId) {
    return walletRepository.findByUserId(userId)
        .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
  }
}
