package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.UserRegistrationRequest;
import com.rogerio.wallet_api.dto.response.UserRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final Clock clock;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  public UserService(UserRepository userRepository,
                     WalletRepository walletRepository,
                     Clock clock,
                     PasswordEncoder passwordEncoder,
                     UserMapper userMapper) {
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.clock = clock;
    this.passwordEncoder = passwordEncoder;
    this.userMapper = userMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  public UserRegistrationResponse register(UserRegistrationRequest request) {
    if (userRepository.existsByEmail(request.email())){
      throw new EmailAlreadyExistsException("Email already registered in the system");
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    User user = userMapper.toEntity(request, encodedPassword, Instant.now(clock));

    User savedUser = userRepository.save(user);

    Wallet wallet = new Wallet(savedUser, savedUser.getCreatedAt());
    Wallet savedWallet = walletRepository.save(wallet);

    return userMapper.toDTO(savedUser, savedWallet);
  }
}
