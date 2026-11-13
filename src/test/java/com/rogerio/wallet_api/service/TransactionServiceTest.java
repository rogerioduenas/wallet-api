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
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import com.rogerio.wallet_api.exception.*;
import com.rogerio.wallet_api.mapper.TransactionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  private UserRepository userRepository;

  @Mock
  private Authentication authentication;

  @Mock
  private TransactionMapper transactionMapper;

  private final Instant fixedInstant = Instant.parse("2026-10-15T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private UUID USER_ID;
  private Wallet WALLET;

  private TransactionService transactionService;

  @BeforeEach
  void setUp() {
    transactionService = new TransactionService(walletRepository, transactionRepository, userRepository, transactionMapper, clock);
    USER_ID = UUID.randomUUID();
    WALLET = new Wallet();
    WALLET.setBalance(BigDecimal.valueOf(00.0));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("Deposit")
  class DepositTests {

    @Test
    @DisplayName("Should successfully deposit amount and update wallet balance when valid request and authenticated user are provided")
    void givenValidRequestAndAuthenticatedUser_whenDeposit_thenSaveTransactionAndUpdateWallet() {
      // Given
      mockAuthenticatedUser(USER_ID);
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));

      BigDecimal initialBalance = WALLET.getBalance();
      BigDecimal depositAmount = request.amount();
      BigDecimal expectedBalance = initialBalance.add(depositAmount);

      TransactionResponse expectedResponse = new TransactionResponse(UUID.randomUUID(), fixedInstant);

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));

      given(transactionRepository.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
      given(transactionMapper.toDto(any(Transaction.class))).willReturn(expectedResponse);

      // When
      TransactionResponse response = transactionService.deposit(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response).isEqualTo(expectedResponse);
      assertThat(WALLET.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated in security context")
    void givenUnauthenticatedUser_whenDeposit_thenThrowAuthenticationCredentialsNotFoundException() {
      // Given
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));

      // When & Then
      assertThatThrownBy(() -> transactionService.deposit(request)).isInstanceOf(AuthenticationCredentialsNotFoundException.class).hasMessage("User is not authenticated");

      then(walletRepository).shouldHaveNoInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet does not exist for the authenticated user ID")
    void givenNonExistentWallet_whenDeposit_thenThrowWalletNotFoundException() {
      // Given
      mockAuthenticatedUser(USER_ID);
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> transactionService.deposit(request)).isInstanceOf(WalletNotFoundException.class).hasMessage("Wallet not found");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should correctly accumulate amount to existing wallet balance when deposit is performed")
    void givenExistingBalance_whenDeposit_thenSumAmountToWalletBalance() {
      // Given
      mockAuthenticatedUser(USER_ID);
      DepositRequest request = new DepositRequest(BigDecimal.valueOf(100.00));
      WALLET.setBalance(BigDecimal.valueOf(50.00));

      BigDecimal expectedBalance = WALLET.getBalance().add(request.amount());

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));
      given(transactionRepository.save(any(Transaction.class))).willAnswer(inv -> inv.getArgument(0));
      given(transactionMapper.toDto(any(Transaction.class))).willReturn(mock(TransactionResponse.class));

      // When
      transactionService.deposit(request);

      // Then
      assertThat(WALLET.getBalance()).isEqualByComparingTo(expectedBalance);
    }
  }

  @Nested
  @DisplayName("Withdraw")
  class WithdrawTests {

    @Test
    @DisplayName("Should successfully withdraw amount and update wallet balance when valid request and sufficient balance are provided")
    void givenValidRequestAndSufficientBalance_whenWithdraw_thenSaveTransactionAndSubtractWalletBalance() {
      // Given
      mockAuthenticatedUser(USER_ID);
      WALLET.setBalance(BigDecimal.valueOf(200.00));

      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50.00));
      BigDecimal expectedBalance = BigDecimal.valueOf(150.00);

      TransactionResponse expectedResponse = new TransactionResponse(UUID.randomUUID(), fixedInstant);

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));
      given(transactionRepository.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
      given(transactionMapper.toDto(any(Transaction.class))).willReturn(expectedResponse);

      // When
      TransactionResponse response = transactionService.withdraw(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response).isEqualTo(expectedResponse);
      assertThat(WALLET.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when wallet balance is less than requested withdraw amount")
    void givenInsufficientBalance_whenWithdraw_thenThrowInsufficientBalanceException() {
      // Given
      mockAuthenticatedUser(USER_ID);
      WALLET.setBalance(BigDecimal.valueOf(30.00));

      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50.00));

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(WALLET));

      // When & Then
      assertThatThrownBy(() -> transactionService.withdraw(request)).isInstanceOf(InsufficientBalanceException.class).hasMessage("Insufficient balance");

      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated during withdraw")
    void givenUnauthenticatedUser_whenWithdraw_thenThrowAuthenticationCredentialsNotFoundException() {
      // Given
      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50.00));

      // When & Then
      assertThatThrownBy(() -> transactionService.withdraw(request)).isInstanceOf(AuthenticationCredentialsNotFoundException.class).hasMessage("User is not authenticated");

      then(walletRepository).shouldHaveNoInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet does not exist during withdraw")
    void givenNonExistentWallet_whenWithdraw_thenThrowWalletNotFoundException() {
      // Given
      mockAuthenticatedUser(USER_ID);
      WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50.00));

      given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> transactionService.withdraw(request)).isInstanceOf(WalletNotFoundException.class).hasMessage("Wallet not found");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Transfer")
  class TransferTests {
    private UUID SENDER_ID;
    private UUID RECEIVER_ID;

    @BeforeEach
    void setUp() {
      SENDER_ID = UUID.randomUUID();
      RECEIVER_ID = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should transfer successfully when valid request and sufficient balance")
    void givenValidTransferRequest_whenTransfer_thenSuccess() {
      // Given
      TransferRequest request = new TransferRequest(RECEIVER_ID, BigDecimal.valueOf(50.00));
      mockAuthenticatedUser(SENDER_ID);

      Wallet senderWallet = new Wallet();
      senderWallet.setBalance(BigDecimal.valueOf(200.00));
      Wallet receiverWallet = new Wallet();
      receiverWallet.setBalance(BigDecimal.valueOf(50.00));

      BigDecimal expectedSenderBalance = BigDecimal.valueOf(150.00);
      BigDecimal expectedReceiverBalance = BigDecimal.valueOf(100.00);

      given(walletRepository.findByUserId(SENDER_ID)).willReturn(Optional.of(senderWallet));
      given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(new User()));
      given(walletRepository.findByUserId(RECEIVER_ID)).willReturn(Optional.of(receiverWallet));

      // When
      transactionService.transfer(request);

      // Then
      assertThat(senderWallet.getBalance()).isEqualByComparingTo(expectedSenderBalance);
      assertThat(receiverWallet.getBalance()).isEqualByComparingTo(expectedReceiverBalance);
    }

    @Test
    @DisplayName("Should throw SelfTransferException when sender and recipient are the same")
    void givenSameSenderAndRecipient_whenTransfer_thenThrowSelfTransferException() {
      // Given
      UUID sameId = SENDER_ID;
      TransferRequest request = new TransferRequest(sameId, BigDecimal.valueOf(50.00));
      mockAuthenticatedUser(SENDER_ID);

      // When & Then
      assertThatThrownBy(() -> transactionService.transfer(request))
          .isInstanceOf(SelfTransferException.class)
          .hasMessage("You can't transfer funds to yourself");

      then(walletRepository).shouldHaveNoInteractions();
      then(userRepository).shouldHaveNoInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();

    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when recipient wallet does not exist")
    void givenNonExistentWalletRecipient_whenTransfer_thenThrowWalletNotFoundException() {
      // Given
      TransferRequest request = new TransferRequest(RECEIVER_ID, BigDecimal.valueOf(50.00));
      mockAuthenticatedUser(SENDER_ID);

      given(walletRepository.findByUserId(SENDER_ID)).willReturn(Optional.of(new Wallet()));
      given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(new User()));
      given(walletRepository.findByUserId(RECEIVER_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> transactionService.transfer(request))
          .isInstanceOf(WalletNotFoundException.class)
          .hasMessage("Wallet not found");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(userRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when recipient does not exist")
    void givenNonExistentRecipient_whenTransfer_thenThrowUserNotFoundException() {
      // Given
      TransferRequest request = new TransferRequest(RECEIVER_ID, BigDecimal.valueOf(50.00));
      mockAuthenticatedUser(SENDER_ID);

      given(walletRepository.findByUserId(SENDER_ID)).willReturn(Optional.of(new Wallet()));
      given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> transactionService.transfer(request))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("User not found");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw UserBlockedException when recipient user is not active")
    void givenInactiveRecipient_whenTransfer_thenThrowUserBlockedException() {
      // Given
      TransferRequest request = new TransferRequest(RECEIVER_ID, BigDecimal.valueOf(50.00));
      User blockedUser = new User();
      blockedUser.setStatus(Status.BLOCKED);
      mockAuthenticatedUser(SENDER_ID);

      given(walletRepository.findByUserId(SENDER_ID)).willReturn(Optional.of(new Wallet()));
      given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(blockedUser));

      // When & Then
      assertThatThrownBy(() -> transactionService.transfer(request))
          .isInstanceOf(UserBlockedException.class)
          .hasMessage("User is not active");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when sender balance is less than amount")
    void givenInsufficientBalance_whenTransfer_thenThrowInsufficientBalanceException() {
      // Given
      TransferRequest request = new TransferRequest(RECEIVER_ID, BigDecimal.valueOf(100.00));
      Wallet senderWallet = new Wallet();
      senderWallet.setBalance(BigDecimal.valueOf(50.00));
      Wallet recipientWallet = new Wallet();
      mockAuthenticatedUser(SENDER_ID);

      given(walletRepository.findByUserId(SENDER_ID)).willReturn(Optional.of(senderWallet));
      given(userRepository.findById(RECEIVER_ID)).willReturn(Optional.of(new User()));
      given(walletRepository.findByUserId(RECEIVER_ID)).willReturn(Optional.of(recipientWallet));

      // When & Then
      assertThatThrownBy(() -> transactionService.transfer(request))
          .isInstanceOf(InsufficientBalanceException.class)
          .hasMessage("Insufficient balance");

      then(walletRepository).shouldHaveNoMoreInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
      then(transactionMapper).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Statement")
  class StatementTests {

    @Nested
    @DisplayName("Customer")
    class CustomerStatementTests {

      @Test
      @DisplayName("Should successfully return paginated statement when customer is active and wallet exists")
      void givenActiveCustomerAndWallet_whenGetCustomerStatement_thenReturnsPagedStatement() {
        // Given
        mockAuthenticatedUser(USER_ID);

        User activeUser = new User();
        activeUser.setStatus(Status.ACTIVE);

        Wallet wallet = new Wallet();
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        Transaction transaction = new Transaction();
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        TransactionStatementResponse expectedResponse = mock(TransactionStatementResponse.class);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser));
        given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(wallet));

        given(transactionRepository.findAll(any(Specification.class), eq(pageable))).willReturn(transactionPage);
        given(transactionMapper.toStatementDto(transaction)).willReturn(expectedResponse);

        Page<TransactionStatementResponse> response = transactionService.getCustomerStatement(null, null, null, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst()).isEqualTo(expectedResponse);
      }

      @Test
      @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is not authenticated in security context")
      void givenUnauthenticatedUser_whenGetCustomerStatement_thenThrowAuthenticationCredentialsNotFoundException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> transactionService.getCustomerStatement(null, null, null, pageable))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessage("User is not authenticated");

        then(userRepository).shouldHaveNoInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw UserNotFoundException when customer ID from context does not exist in repository")
      void givenNonExistentUser_whenGetCustomerStatement_thenThrowUserNotFoundException() {
        // Given
        mockAuthenticatedUser(USER_ID);
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getCustomerStatement(null, null, null, pageable))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("User not found");

        then(walletRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw UserBlockedException when customer account status is not ACTIVE")
      void givenInactiveUser_whenGetCustomerStatement_thenThrowUserBlockedException() {
        // Given
        mockAuthenticatedUser(USER_ID);
        Pageable pageable = PageRequest.of(0, 10);

        User inactiveUser = new User();
        inactiveUser.setStatus(Status.BLOCKED);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(inactiveUser));

        // When & Then
        assertThatThrownBy(() -> transactionService.getCustomerStatement(null, null, null, pageable))
            .isInstanceOf(UserBlockedException.class)
            .hasMessage("User is not active");

        then(walletRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw WalletNotFoundException when active user does not have an associated wallet")
      void givenActiveUserWithoutWallet_whenGetCustomerStatement_thenThrowWalletNotFoundException() {
        // Given
        mockAuthenticatedUser(USER_ID);
        Pageable pageable = PageRequest.of(0, 10);
        User activeUser = new User();
        activeUser.setStatus(Status.ACTIVE);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser));
        given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getCustomerStatement(null, null, null, pageable))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessage("Wallet not found");

        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should apply filters correctly when startDate, endDate and type are provided")
      void givenFiltersProvided_whenGetCustomerStatement_thenAppliesSpecificationFilters() {
        // Given
        mockAuthenticatedUser(USER_ID);
        User activeUser = new User();
        activeUser.setStatus(Status.ACTIVE);

        Wallet wallet = new Wallet();
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        Instant startDate = Instant.parse("2026-10-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-10-31T00:00:00Z");
        TransactionType type = TransactionType.DEPOSIT;

        Page<Transaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser));
        given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(wallet));

        given(transactionRepository.findAll(any(Specification.class), eq(pageable))).willReturn(emptyPage);

        // When
        Page<TransactionStatementResponse> result = transactionService.getCustomerStatement(startDate, endDate, type, pageable);

        // Then
        assertThat(result).isNotNull();

        then(transactionRepository).should().findAll(any(Specification.class), eq(pageable));
      }
    }

    @Nested
    @DisplayName("Admin")
    class AdminStatementTests {

      private static final UUID TARGET_USER_ID = UUID.randomUUID();

      @Test
      @DisplayName("Should successfully return paginated statement when target user and wallet exist")
      void givenValidTargetUserAndWallet_whenGetStatementForAdmin_thenReturnsPagedStatement() {
        // Given
        User targetUser = new User();
        ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);

        Wallet wallet = new Wallet();
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        Transaction transaction = new Transaction();
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        TransactionStatementResponse expectedResponse = mock(TransactionStatementResponse.class);

        given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
        given(walletRepository.findByUserId(TARGET_USER_ID)).willReturn(Optional.of(wallet));
        given(transactionRepository.findAll(any(Specification.class), eq(pageable))).willReturn(transactionPage);
        given(transactionMapper.toStatementDto(transaction)).willReturn(expectedResponse);

        // When
        Page<TransactionStatementResponse> response = transactionService.getStatementForAdmin(TARGET_USER_ID, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst()).isEqualTo(expectedResponse);
      }

      @Test
      @DisplayName("Should throw UserNotFoundException when target user ID does not exist in repository")
      void givenNonExistentTargetUser_whenGetStatementForAdmin_thenThrowUserNotFoundException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getStatementForAdmin(TARGET_USER_ID, pageable))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("User not found");

        then(walletRepository).shouldHaveNoInteractions();
        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("Should throw WalletNotFoundException when target user does not have an associated wallet")
      void givenTargetUserWithoutWallet_whenGetStatementForAdmin_thenThrowWalletNotFoundException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        User targetUser = new User();
        ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
        targetUser.setStatus(Status.ACTIVE);

        given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
        given(walletRepository.findByUserId(TARGET_USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getStatementForAdmin(TARGET_USER_ID, pageable))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessage("Wallet not found");

        then(transactionRepository).shouldHaveNoInteractions();
        then(transactionMapper).shouldHaveNoInteractions();
      }
    }
  }

  private void mockAuthenticatedUser(UUID userId) {
    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getName()).willReturn(userId.toString());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}




