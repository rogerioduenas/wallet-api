package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AuthLoginRequest;
import com.rogerio.wallet_api.dto.response.AuthTokenResponse;
import com.rogerio.wallet_api.exception.InvalidCredentialsException;
import com.rogerio.wallet_api.exception.UserBlockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final  TokenService tokenService;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  public AuthTokenResponse login(AuthLoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() ->
            new InvalidCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    if (user.getStatus().equals(Status.BLOCKED)) {
      throw new UserBlockedException("User is blocked");
    }

    String role = user.getRole().name();
    String token = tokenService.generateToken(user.getEmail(), role);
    long expirationInMs = tokenService.getExpirationInMs();

    return new AuthTokenResponse(token, "Bearer", role, expirationInMs);
  }
}
