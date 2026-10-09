package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.UserRegistrationRequest;
import com.rogerio.wallet_api.dto.response.UserRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private UserMapper userMapper;

  @Mock
  private UserRepository userRepository;

  @Mock
  private WalletRepository walletRepository;

  private final Instant fixedInstant = Instant.parse("2026-10-07T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private UserService userService;

  private final String VALID_NAME = "Mike";
  private final String VALID_EMAIL = "valid@email.com";
  private final String VALID_PASSWORD = "password";
  private final UserRegistrationRequest VALID_REQUEST = new UserRegistrationRequest(VALID_NAME, VALID_EMAIL, VALID_PASSWORD);

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, walletRepository, clock, passwordEncoder, userMapper);
  }

  @Test
  @DisplayName("Should successfully register user and wallet when request data is valid")
  void givenValidRegistrationRequest_whenRegister_thenSaveUserAndWalletAndReturnResponse() {
    // Given
    String encodedPassword = "encodedPassword";
    UUID userId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();

    User savedUser = new User(VALID_NAME, VALID_EMAIL, encodedPassword, fixedInstant);
    ReflectionTestUtils.setField(savedUser, "id", userId);

    Wallet savedWallet = new Wallet(savedUser, fixedInstant);
    ReflectionTestUtils.setField(savedWallet, "id", walletId);

    UserRegistrationResponse expectedResponse = new UserRegistrationResponse(
        userId, VALID_NAME, VALID_EMAIL, walletId, fixedInstant
    );

    given(userRepository.existsByEmail(VALID_REQUEST.email())).willReturn(false);
    given(passwordEncoder.encode(VALID_REQUEST.password())).willReturn(encodedPassword);
    given(userMapper.toEntity(VALID_REQUEST, encodedPassword, fixedInstant)).willReturn(savedUser);
    given(userRepository.save(savedUser)).willReturn(savedUser);
    given(walletRepository.save(any(Wallet.class))).willReturn(savedWallet);
    given(userMapper.toDTO(savedUser, savedWallet)).willReturn(expectedResponse);

    // When
    UserRegistrationResponse response = userService.register(VALID_REQUEST);

    // Then
    assertThat(response).isEqualTo(expectedResponse);

    then(userRepository).should().save(savedUser);
    then(walletRepository).should().save(any(Wallet.class));
  }

  @Test
  @DisplayName("Should throw EmailAlreadyExistsException if email already registered in the system")
  void givenExistingEmail_whenRegister_thenThrowEmailAlreadyExistsException() {
    // Given
    String existingEmail = "existing@email.com";
    given(userRepository.existsByEmail(existingEmail)).willReturn(true);
    UserRegistrationRequest requestWithExistingEmail = new UserRegistrationRequest(
        VALID_NAME,
        existingEmail,
        VALID_PASSWORD
    );

    // When & Then
    assertThatThrownBy(() -> userService.register(requestWithExistingEmail))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessage("Email already registered in the system");

    then(userRepository).should(never()).save(any(User.class));
    then(walletRepository).should(never()).save(any(Wallet.class));
  }

  @Test
  @DisplayName("Should throw Exception and not save wallet when userRepository save fails")
  void givenUserRepositoryError_whenRegister_thenThrowExceptionAndDoNotSaveWallet() {
    // Given
    given(userRepository.save(any(User.class))).willThrow(new RuntimeException());

    // When & Then
    assertThatThrownBy(() -> userService.register(VALID_REQUEST))
        .isInstanceOf(RuntimeException.class);

    then(walletRepository).should(never()).save(any(Wallet.class));
  }

  @Test
  @DisplayName("Should throw Exception when walletRepository save fails")
  void givenWalletRepositoryError_whenRegister_thenThrowException() {
    // Given
    User dummyUser = new User(VALID_NAME, VALID_EMAIL, "encodedPassword", fixedInstant);

    given(userRepository.existsByEmail(VALID_REQUEST.email())).willReturn(false);
    given(passwordEncoder.encode(VALID_REQUEST.password())).willReturn("encodedPassword");
    given(userMapper.toEntity(VALID_REQUEST, "encodedPassword", fixedInstant)).willReturn(dummyUser);
    given(userRepository.save(dummyUser)).willReturn(dummyUser);

    given(walletRepository.save(any(Wallet.class))).willThrow(new RuntimeException("Database error"));

    // When & Then
    assertThatThrownBy(() -> userService.register(VALID_REQUEST))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database error");

    then(userRepository).should().save(dummyUser);
  }
}