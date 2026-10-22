package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AuthLoginRequest;
import com.rogerio.wallet_api.dto.response.AuthTokenResponse;
import com.rogerio.wallet_api.exception.InvalidCredentialsException;
import jakarta.validation.constraints.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TokenService tokenService;

  @InjectMocks
  private AuthService authService;

  private User mockUser;
  private AuthLoginRequest loginRequest;

  @BeforeEach
  void setUp() {
    mockUser = new User();
    mockUser.setEmail("email@email.com");
    mockUser.setPassword("$2a$10$encodedPasswordHash");
    mockUser.setRole(Role.ROLE_USER);

    loginRequest = new AuthLoginRequest("email@email.com", "password");
  }

  @Test
  @DisplayName("Should authenticate successfully and return token response when credentials are valid")
  void shouldAuthenticateSuccessfullyWhenCredentialsAreValid() {
    // Given
    given(userRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(mockUser));
    given(passwordEncoder.matches(loginRequest.password(), mockUser.getPassword())).willReturn(true);

    given(tokenService.generateToken(mockUser.getEmail(), "ROLE_USER")).willReturn("mocked.jwt.token");
    given(tokenService.getExpirationInMs()).willReturn(3600000L);

    // When
    AuthTokenResponse response = authService.login(loginRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.token()).isEqualTo("mocked.jwt.token");
    assertThat(response.type()).isEqualTo("Bearer");
    assertThat(response.role()).isEqualTo("ROLE_USER");
    assertThat(response.expiresIn()).isEqualTo(3600000L);

    then(userRepository).should().findByEmail(loginRequest.email());
    then(passwordEncoder).should().matches(loginRequest.password(), mockUser.getPassword());
    then(tokenService).should().generateToken(mockUser.getEmail(), "ROLE_USER");
  }

  @Test
  @DisplayName("Should throw InvalidCredentialsException when email does not exist")
  void shouldThrowExceptionWhenEmailNotFound() {
    // Given
    given(userRepository.findByEmail(any(String.class))).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid email or password");
  }

  @Test
  @DisplayName("Should throw InvalidCredentialsException when password does not match")
  void shouldThrowExceptionWhenPasswordDoesNotMatch() {
    // Given
    given(userRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(mockUser));
    given(passwordEncoder.matches(loginRequest.password(), mockUser.getPassword())).willReturn(false);

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid email or password");
  }
}
